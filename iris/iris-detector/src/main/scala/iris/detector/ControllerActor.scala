package iris.detector

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import com.typesafe.config.Config
import csw.command.client.CommandResponseManager
import csw.event.api.scaladsl.EventPublisher
import csw.framework.models.CswContext
import csw.params.commands.CommandIssue.UnsupportedCommandIssue
import csw.params.commands.CommandResponse.{Accepted, Completed, Invalid}
import csw.params.core.models.Id
import csw.params.events.{IRDetectorEvent, ObserveEvent}
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import csw.time.scheduler.api.TimeServiceScheduler
import iris.detector.commands.{ControllerMessage, FitsData, FitsMessage}
import iris.detector.commands.ControllerMessage._
import iris.detector.commands.FitsMessage.WriteData

import java.time.Instant

class ControllerActor(cswContext: CswContext, config: Config) {
  private val crm: CommandResponseManager                = cswContext.commandResponseManager
  private val timeServiceScheduler: TimeServiceScheduler = cswContext.timeServiceScheduler
  private val eventPublisher: EventPublisher             = cswContext.eventService.defaultPublisher
  private val detectorPrefix: Prefix                     = cswContext.componentInfo.prefix
  private val detectorDimensions: (Int, Int)             = (config.getInt("xs"), config.getInt("ys"))
  private val log                                        = cswContext.loggerFactory.getLogger

  private def generateFakeImageData(xs: Int, ys: Int) = Array.tabulate(xs, ys)((x, y) => x * xs + y)

  def uninitialized: Behavior[ControllerMessage] = {
    log.info("Detector is now in 'UnInitialised' state")
    receiveWithDefaultBehavior("UnInitialised") {
      case Initialize(runId) =>
        crm.updateCommand(Completed(runId))
        idle
      case IsValid(runId, commandName, replyTo) if commandName == Constants.Initialize =>
        replyTo ! Accepted(runId)
        Behaviors.same
    }
  }

  private lazy val idleHandler: PartialFunction[ControllerMessage, Behavior[ControllerMessage]] = {
    case ConfigureExposure(runId, exposureId, filename, ramps, rampIntegrationTime) =>
      crm.updateCommand(Completed(runId))
      loaded(ControllerData(filename, exposureId, ramps, rampIntegrationTime, 0))
    case Shutdown(runId) =>
      crm.updateCommand(Completed(runId))
      uninitialized
    case IsValid(runId, commandName, replyTo) if commandName == Constants.Shutdown || commandName == Constants.LoadConfiguration =>
      replyTo ! Accepted(runId)
      Behaviors.same
  }

  private def idle: Behavior[ControllerMessage] = {
    log.info("Detector is now in 'Non-Configured' state")
    receiveWithDefaultBehavior("Non-Configured")(idleHandler)
  }

  private def loaded(data: ControllerData): Behavior[ControllerMessage] = Behaviors.setup { ctx =>
    log.info(s"Detector is now in 'Configured' state with config $data")
    receiveWithDefaultBehavior("configured") {
      idleHandler.orElse {
        case StartExposure(runId, replyTo) =>
          ctx.self ! ExposureInProgress(runId)
          eventPublisher.publish(IRDetectorEvent.exposureStart(detectorPrefix, data.exposureId))
          exposureInProgress(data.resetRamp(), replyTo)
        case IsValid(runId, commandName, replyTo) if commandName == Constants.StartExposure =>
          replyTo ! Accepted(runId)
          Behaviors.same
      }
    }
  }

  private def exposureInProgress(data: ControllerData, replyTo: ActorRef[FitsMessage]): Behavior[ControllerMessage] =
    Behaviors.setup { ctx =>
      log.info("Exposure Started and Detector is now in 'Exposing' state")
      var isExposureRunning = true

      def finishExposure(runId: Id, event: ObserveEvent): Unit = {
        isExposureRunning = false
        eventPublisher.publish(event)
        val fitsData = FitsData(generateFakeImageData(detectorDimensions._1, detectorDimensions._2))
        replyTo ! WriteData(runId, fitsData, data.exposureId, data.filename)
      }

      receiveWithDefaultBehavior("exposing") {
        case ExposureInProgress(runId) if isExposureRunning =>
          // publish ObserveEvent
          eventPublisher.publish(
            IRDetectorEvent.exposureData(
              detectorPrefix,
              data.exposureId,
              1,
              1,
              data.ramps,
              data.currentRamp,
              data.ramps * data.rampIntegrationTime,
              calculateTimeRemaining(data)
            )
          )
          if (data.currentRamp == data.ramps) ctx.self ! ExposureFinished(runId)
          else {
            timeServiceScheduler.scheduleOnce(UTCTime(Instant.ofEpochMilli(System.currentTimeMillis() + data.rampIntegrationTime))) {
              ctx.self ! ExposureInProgress(runId)
            }
          }
          exposureInProgress(data.incrementRamp(), replyTo)
        case ExposureInProgress(runId) if !isExposureRunning =>
          crm.updateCommand(Completed(runId))
          loaded(data)
        case AbortExposure(runId) =>
          log.info(s"Exposure Aborted for runId $runId")
          finishExposure(runId, IRDetectorEvent.exposureAborted(detectorPrefix, data.exposureId))
          loaded(data)
        case Shutdown(runId) =>
          log.info(s"Exposure Aborted before shutting down for runId $runId")
          finishExposure(runId, IRDetectorEvent.exposureAborted(detectorPrefix, data.exposureId))
          uninitialized
        case ExposureFinished(runId) =>
          log.info(s"Exposure Finished for runId $runId")
          finishExposure(runId, IRDetectorEvent.exposureEnd(detectorPrefix, data.exposureId))
          loaded(data)
        case IsValid(runId, commandName, replyTo) if commandName == Constants.Shutdown || commandName == Constants.AbortExposure =>
          replyTo ! Accepted(runId)
          Behaviors.same
      }
    }

  private def receiveWithDefaultBehavior(
      state: String
  )(handle: PartialFunction[ControllerMessage, Behavior[ControllerMessage]]): Behavior[ControllerMessage] =
    Behaviors.receiveMessage(handle.orElse {
      case IsValid(runId, command, replyTo) =>
        val errMsg = s"Command: ${command.name} is not valid in $state state."
        replyTo ! Invalid(runId, UnsupportedCommandIssue(errMsg))
        Behaviors.same
      case _ => Behaviors.unhandled
    })

  private def calculateTimeRemaining(data: ControllerData) = (data.ramps - data.currentRamp) * data.rampIntegrationTime
}

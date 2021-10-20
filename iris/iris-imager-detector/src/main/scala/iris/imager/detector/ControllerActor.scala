package iris.imager.detector

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.typesafe.config.Config
import csw.framework.models.CswContext
import csw.params.commands.CommandResponse.Completed
import csw.params.core.models.Id
import csw.params.events.IRDetectorEvent
import csw.time.core.models.UTCTime
import iris.imager.detector.commands.ControllerMessage._
import iris.imager.detector.commands.FitsMessage.WriteData
import iris.imager.detector.commands.{ControllerMessage, FitsData, FitsMessage}

import java.time.Instant

class ControllerActor(cswContext: CswContext, config: Config) {
  val crm                  = cswContext.commandResponseManager
  val timeServiceScheduler = cswContext.timeServiceScheduler
  val eventPublisher       = cswContext.eventService.defaultPublisher

  private lazy val detectorDimensions: (Int, Int) = {
    (config.getInt("xs"), config.getInt("ys"))
  }

  private def generateFakeImageData(xs: Int, ys: Int) = {
    Array.tabulate(xs, ys) { case (x, y) =>
      x * xs + y
    }
  }

  lazy val uninitialized: Behavior[ControllerMessage] =
    Behaviors.receiveMessage {
      case Initialize(runId) =>
        crm.updateCommand(Completed(runId))
        idle
        
      case _ => Behaviors.unhandled
    }

  lazy val idle: Behavior[ControllerMessage] =
    Behaviors.receiveMessage {
      case ConfigureExposure(runId, exposureId, filename, ramps, rampIntegrationTime) =>
        crm.updateCommand(Completed(runId))
        loaded(ControllerData(filename, exposureId, ramps, rampIntegrationTime))
      case Shutdown(runId) =>
        crm.updateCommand(Completed(runId))
        uninitialized
      case _ => Behaviors.unhandled
    }

  def loaded(data: ControllerData): Behavior[ControllerMessage] = Behaviors.receive { (ctx, msg) =>
    msg match {
      case StartExposure(runId, replyTo) =>
        ctx.self ! ExposureInProgress(runId, 0)
        eventPublisher.publish(IRDetectorEvent.exposureStart(cswContext.componentInfo.prefix, data.exposureId))
        exposureInProgress(data, replyTo)
      case ConfigureExposure(runId, exposureId, filename, ramps, rampIntegrationTime) =>
        crm.updateCommand(Completed(runId))
        loaded(ControllerData(filename, exposureId, ramps, rampIntegrationTime))
      case Shutdown(runId) =>
        crm.updateCommand(Completed(runId))
        uninitialized
      case _ => Behaviors.unhandled
    }
  }

  def exposureInProgress(data: ControllerData, replyTo: ActorRef[FitsMessage]): Behavior[ControllerMessage] = {
    var isExposureRunning = true

    def finishExposure(runId: Id): Behavior[ControllerMessage] = {
      isExposureRunning = false
      val prefix = cswContext.componentInfo.prefix
      eventPublisher.publish(IRDetectorEvent.exposureEnd(prefix, data.exposureId))
      val fitsData = FitsData(generateFakeImageData(detectorDimensions._1, detectorDimensions._2))
      replyTo ! WriteData(runId, fitsData, data.exposureId, data.filename)
      loaded(data)
    }

    Behaviors.receive { (ctx, msg) =>
      msg match {
        case ExposureInProgress(runId, currentRamp) if isExposureRunning =>
          if (currentRamp == data.ramps) {
            ctx.self ! ExposureFinished(runId)
          }
          else {
            val instant = Instant.ofEpochMilli(System.currentTimeMillis() + (data.rampIntegrationTime))
            timeServiceScheduler.scheduleOnce(UTCTime(instant)) {
              ctx.self ! ExposureInProgress(runId, currentRamp + 1)
            }
          }
          Behaviors.same
        case ExposureInProgress(runId, _) if !isExposureRunning =>
          crm.updateCommand(Completed(runId))
          loaded(data)
        case AbortExposure(runId) =>
          finishExposure(runId)
        case ExposureFinished(runId) =>
          finishExposure(runId)
        case _ => Behaviors.unhandled
      }
    }
  }
}

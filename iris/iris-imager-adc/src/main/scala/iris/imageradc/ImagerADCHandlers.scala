package iris.imageradc

import akka.actor.typed.Scheduler
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.params.commands.CommandIssue.UnsupportedCommandIssue
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandName, ControlCommand, Setup}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import iris.imageradc.commands.PrismCommands.IsValid
import iris.imageradc.commands.{ADCCommand, PrismCommands}
import iris.imageradc.events.PrismStateEvent
import iris.imageradc.models.{AssemblyConfiguration, PrismState}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext}

class ImagerADCHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._
  implicit val a: Scheduler = ctx.system.scheduler

  implicit val ec: ExecutionContext  = ctx.executionContext
  private val log                    = loggerFactory.getLogger
  private val adcImagerConfiguration = AssemblyConfiguration(ctx.system.settings.config.getConfig("iris.imager.ADC"))
  private val adcActor               = ctx.spawnAnonymous(PrismActor.behavior(cswCtx, adcImagerConfiguration, log))

  override def initialize(): Unit = {
    log.info("Initializing imager.adc...")
    cswCtx.eventService.defaultPublisher.publish(
      PrismStateEvent.make(PrismState.STOPPED, onTarget = true)
    )
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = {
    controlCommand match {
      case setup: Setup => handleValidation(runId, setup)
      case observe      => Invalid(runId, UnsupportedCommandIssue(s"$observe command not supported."))
    }
  }

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse =
    controlCommand match {
      case setup: Setup => handleSetup(runId, setup)
      case observe      => Invalid(runId, UnsupportedCommandIssue(s"$observe command not supported."))
    }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {}

  override def onShutdown(): Unit = {}

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}

  private def handleSetup(runId: Id, setup: Setup): SubmitResponse =
    setup.commandName match {
      case ADCCommand.RetractSelect =>
        ADCCommand.getPrismPosition(setup) match {
          case Left(commandIssue) => Invalid(runId, commandIssue)
          case Right(value) =>
            adcActor ! PrismCommands.RetractSelect(runId, value)
            Started(runId)
        }
      case ADCCommand.PrismFollow =>
        ADCCommand.getTargetAngle(setup) match {
          case Left(commandIssue) => Invalid(runId, commandIssue)
          case Right(value) =>
            adcActor ! PrismCommands.PrismFollow(value)
            Completed(runId)
        }

      case ADCCommand.PrismStop =>
        adcActor ! PrismCommands.PrismStop(runId)
        Completed(runId)
      case CommandName(name) => Invalid(runId, UnsupportedCommandIssue(s"Setup command: $name not supported."))
    }

  private def handleValidation(runId: Id, setup: Setup): ValidateCommandResponse = {
    val timeout: FiniteDuration = 1.seconds
    implicit val value: Timeout = Timeout(timeout)

    def sendIsValid: ValidateCommandResponse =
      Await.result(adcActor ? (IsValid(runId, setup, _)), timeout)

    setup.commandName match {
      case ADCCommand.RetractSelect =>
        ADCCommand.getPrismPosition(setup) match {
          case Left(commandIssue) =>
            log.error(s"Failed to retrieve prism position, reason: ${commandIssue.reason}")
            Invalid(runId, commandIssue)
          case Right(_) =>
            sendIsValid
        }
      case ADCCommand.PrismFollow =>
        ADCCommand.getTargetAngle(setup) match {
          case Left(commandIssue) =>
            log.error(s"Failed to retrieve target angle, reason: ${commandIssue.reason}")
            Invalid(runId, commandIssue)
          case Right(_) => sendIsValid
        }
      case _ => sendIsValid
    }
  }

}

package iris.imagerfilter

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.params.commands.CommandIssue.UnsupportedCommandIssue
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandName, ControlCommand, Setup}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import iris.imagerfilter.commands.SelectCommand

import scala.concurrent.ExecutionContextExecutor

class ImagerFilterHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._
  implicit val ec: ExecutionContextExecutor = ctx.executionContext
  private val log                           = loggerFactory.getLogger
  private val filterWheelConfiguration      = FilterWheelConfiguration(ctx.system)
  private val imageActor                    = ctx.spawnAnonymous(FilterWheelActor.behavior(cswCtx, filterWheelConfiguration))

  override def initialize(): Unit = {
    log.info("Initializing imager.filter...")
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = Accepted(runId)

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

  private def handleSelect(runId: Id, setup: Setup) =
    SelectCommand.getWheel1TargetPosition(setup) match {
      case Right(targetPosition) =>
        log.info(s"Moving filter wheel to target position: $targetPosition")
        imageActor ! FilterWheelCommand.MoveWheel1(targetPosition, runId)
        Started(runId)
      case Left(commandIssue) =>
        log.error(s"Failed to retrieve target position, reason: ${commandIssue.reason}")
        Invalid(runId, commandIssue)
    }

  private def handleSetup(runId: Id, setup: Setup) =
    setup.commandName match {
      case SelectCommand.Name => handleSelect(runId, setup)
      case CommandName(name) =>
        val errMsg = s"Setup command: $name not supported."
        log.error(errMsg)
        Invalid(runId, UnsupportedCommandIssue(errMsg))
    }

}

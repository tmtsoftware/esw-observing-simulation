package iris.ifsscale

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Scheduler}
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
import iris.ifsscale.ScaleWheelCommand.IsValidMove
import iris.ifsscale.commands.SelectCommand
import iris.ifsscale.events.IfsScaleEvent

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}

class IfsScaleHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._
  implicit val a: Scheduler = ctx.system.scheduler

  implicit val ec: ExecutionContext   = ctx.executionContext
  private val log                     = loggerFactory.getLogger
  private val scaleWheelConfiguration = ScaleWheelConfiguration(ctx.system)
  private val ifsActor                = ctx.spawnAnonymous(ScaleWheelActor.behavior(cswCtx, scaleWheelConfiguration))

  override def initialize(): Unit = {
    log.info("Initializing ifs.scale...")
    cswCtx.eventService.defaultPublisher.publish(
      IfsScaleEvent.make(ScaleWheelActor.InitialScale, ScaleWheelActor.InitialScale)
    )
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = {
    val timeout: FiniteDuration = 1.seconds
    implicit val value: Timeout = Timeout(timeout)
    val eventualValidateResponse: Future[ValidateCommandResponse] = ifsActor ? { x: ActorRef[ValidateCommandResponse] =>
      IsValidMove(runId, x)
    }
    Await.result(eventualValidateResponse, timeout)
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

  private def handleSelect(runId: Id, setup: Setup) =
    SelectCommand.getTargetScale(setup) match {
      case Right(targetPosition) =>
        log.info(s"Moving scale wheel to target position: $targetPosition")
        ifsActor ! ScaleWheelCommand.UpdateScale(targetPosition, runId)
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

package wfos.filter

import akka.actor.typed.Scheduler
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.params.commands.CommandIssue.UnsupportedCommandIssue
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandName, ControlCommand, Setup}
import csw.params.core.models.Id
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import wfos.commons.models.WheelCommand.IsValidMove
import wfos.commons.models.{AssemblyConfiguration, WheelCommand}
import wfos.filter.commands.SelectCommand
import wfos.filter.events.FilterPositionEvent

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext}

class FilterHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._
  implicit val a: Scheduler = ctx.system.scheduler

  implicit val ec: ExecutionContext    = ctx.executionContext
  private val log                      = loggerFactory.getLogger
  private val filterPrefix: Prefix     = cswCtx.componentInfo.prefix
  private val filterWheelConfiguration = AssemblyConfiguration(ConfigFactory.load().getConfig(filterPrefix.toString().toLowerCase()))
  private val filterPositionEvent      = new FilterPositionEvent(filterPrefix)
  private val filterActor              = ctx.spawnAnonymous(FilterWheelActor.behavior(cswCtx, filterWheelConfiguration))

  override def initialize(): Unit = {
    log.info(s"Initializing ${filterPrefix.componentName}...")
    cswCtx.eventService.defaultPublisher.publish(
      filterPositionEvent.make(FilterWheelActor.InitialPosition, FilterWheelActor.InitialPosition, dark = false)
    )
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = {
    val timeout: FiniteDuration = 1.seconds
    implicit val value: Timeout = Timeout(timeout)

    val initialValidateRes = controlCommand match {
      case cmd: Setup => validateSetupCommand(runId, cmd)
      case observe    => Invalid(runId, UnsupportedCommandIssue(s"$observe command not supported."))
    }

    initialValidateRes match {
      case _: Accepted => Await.result(filterActor ? (IsValidMove(runId, _)), timeout)
      case invalidRes  => invalidRes
    }
  }

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse =
    controlCommand match {
      case setup: Setup => handleSelect(runId, setup)
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
        log.info(s"Filter Assembly: Moving wheel to target position: $targetPosition")
        filterActor ! WheelCommand.Move(targetPosition, runId)
        Started(runId)
      case Left(commandIssue) => Invalid(runId, commandIssue)
    }

  private def validateSelect(runId: Id, setup: Setup) =
    SelectCommand.getWheel1TargetPosition(setup) match {
      case Right(_) => Accepted(runId)
      case Left(commandIssue) =>
        log.error(s"Filter Assembly: Failed to retrieve target position, reason: ${commandIssue.reason}")
        Invalid(runId, commandIssue)
    }

  private def validateSetupCommand(runId: Id, setup: Setup) = setup.commandName match {
    case SelectCommand.Name => validateSelect(runId, setup)
    case CommandName(name) =>
      val errMsg = s"Filter Assembly: Setup command: $name not supported."
      log.error(errMsg)
      Invalid(runId, UnsupportedCommandIssue(errMsg))
  }

}

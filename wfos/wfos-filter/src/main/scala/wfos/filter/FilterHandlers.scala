package wfos.filter

import akka.actor.typed.{ActorRef, Scheduler}
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
import wfos.filter.commands.{BlueSelectCommand, RedSelectCommand, SelectCommand}
import wfos.filter.events.{BlueFilterPositionEvent, FilterPositionEvent, RedFilterPositionEvent}
import wfos.filter.models.WheelCommand.IsValidMove
import wfos.filter.models.{AssemblyConfiguration, BlueFilterWheelPosition, FilterWheelPosition, RedFilterWheelPosition, WheelCommand}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext}

abstract class FilterHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._
  implicit val a: Scheduler = ctx.system.scheduler

  implicit val ec: ExecutionContext = ctx.executionContext
  private val log                   = loggerFactory.getLogger
  private val filterPrefix: Prefix  = cswCtx.componentInfo.prefix

  val filterPositionEvent: FilterPositionEvent
  val filterActor: ActorRef[WheelCommand]
  val initialPosition: FilterWheelPosition
  val selectCommand: SelectCommand

  override def initialize(): Unit = {
    log.info(s"Initializing ${filterPrefix.componentName}...")
    cswCtx.eventService.defaultPublisher.publish(
      filterPositionEvent.make(initialPosition, initialPosition, dark = false)
    )
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = {
    val timeout: FiniteDuration = 1.seconds
    implicit val value: Timeout = Timeout(timeout)

    val validateParamsRes = controlCommand match {
      case cmd: Setup => validateSetupParams(runId, cmd)
      case observe    => Invalid(runId, UnsupportedCommandIssue(s"$observe command not supported."))
    }

    validateParamsRes match {
      case _: Accepted => Await.result(filterActor ? (IsValidMove(runId, _)), timeout)
      case invalidRes  => invalidRes
    }
  }

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse =
    controlCommand match {
      case setup: Setup => handleSelect(runId, setup)
      case observe      => Invalid(runId, UnsupportedCommandIssue(s"$observe command not supported."))
    }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {}

  override def onShutdown(): Unit = {}

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}

  private def handleSelect(runId: Id, setup: Setup) =
    selectCommand.getWheel1TargetPosition(setup) match {
      case Right(targetPosition) =>
        log.info(s"Filter Assembly: Moving wheel to target position: $targetPosition")
        filterActor ! WheelCommand.Move(targetPosition, runId)
        Started(runId)
      case Left(commandIssue) => Invalid(runId, commandIssue)
    }

  private def validateSelectParams(runId: Id, setup: Setup) =
    selectCommand.getWheel1TargetPosition(setup) match {
      case Right(_) => Accepted(runId)
      case Left(commandIssue) =>
        log.error(s"Filter Assembly: Failed to retrieve target position, reason: ${commandIssue.reason}")
        Invalid(runId, commandIssue)
    }

  private def validateSetupParams(runId: Id, setup: Setup) = setup.commandName match {
    case selectCommand.Name => validateSelectParams(runId, setup)
    case CommandName(name) =>
      val errMsg = s"Filter Assembly: Setup command: $name not supported."
      log.error(errMsg)
      Invalid(runId, UnsupportedCommandIssue(errMsg))
  }

}

class RedFilterHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends FilterHandlers(ctx, cswCtx) {
  private val prefix: Prefix = cswCtx.componentInfo.prefix
  private val filterWheelConfiguration = AssemblyConfiguration(
    ConfigFactory.load().getConfig(prefix.toString().toLowerCase())
  )
  override val filterPositionEvent: FilterPositionEvent = new RedFilterPositionEvent(prefix)
  override val filterActor: ActorRef[WheelCommand] =
    ctx.spawnAnonymous(new RedFilterWheelActor(cswCtx, filterWheelConfiguration).behavior)
  override val initialPosition: FilterWheelPosition = RedFilterWheelPosition.RPrime
  override val selectCommand: SelectCommand         = RedSelectCommand
}

class BlueFilterHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends FilterHandlers(ctx, cswCtx) {
  private val prefix: Prefix = cswCtx.componentInfo.prefix
  private val filterWheelConfiguration = AssemblyConfiguration(
    ConfigFactory.load().getConfig(prefix.toString().toLowerCase())
  )
  override val filterPositionEvent: FilterPositionEvent = new BlueFilterPositionEvent(prefix)
  override val filterActor: ActorRef[WheelCommand] =
    ctx.spawnAnonymous(new BlueFilterWheelActor(cswCtx, filterWheelConfiguration).behavior)
  override val initialPosition: FilterWheelPosition = BlueFilterWheelPosition.UPrime
  override val selectCommand: SelectCommand         = BlueSelectCommand
}

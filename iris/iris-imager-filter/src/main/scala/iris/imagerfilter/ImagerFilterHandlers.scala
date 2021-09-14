package iris.imagerfilter

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandIssue, ControlCommand}
import csw.params.core.generics.Parameter
import csw.params.core.models.Id
import csw.time.core.models.UTCTime

import scala.concurrent.ExecutionContextExecutor

/**
 * Domain specific logic should be written in below handlers.
 * This handlers gets invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to Irishcd,
 * This will be first validated in the supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw/commons/framework.html
 */
class ImagerFilterHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._
  implicit val ec: ExecutionContextExecutor = ctx.executionContext
  private val log                           = loggerFactory.getLogger
  private val imageActor                    = ctx.spawnAnonymous(FilterWheelActor.behavior(cswCtx))

  override def initialize(): Unit = {
    log.info("Initializing imager.filter...")

  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = Accepted(runId)

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = {
    val parameter         = controlCommand.paramSet.find(_.keyName == "wheel1").get.asInstanceOf[Parameter[String]]
    val targetPos: String = parameter.values.head

    if (FilterWheelActor.positions.contains(targetPos)) {
      imageActor ! FilterWheelActorMessage.Wheel1(targetPos)
      Started(runId)
    }
    else Invalid(runId, CommandIssue.ParameterValueOutOfRangeIssue(s"$targetPos is not a valid position"))
  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {}

  override def onShutdown(): Unit = {}

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}

}

class FilterWheelActor(cswContext: CswContext) {

  private def getCurrentIndexAndDiff(currentPos: String, targetPos: String): (Int, Int) = {
    val currentPosIndex = FilterWheelActor.positions.indexOf(currentPos)
    val targetPosIndex  = FilterWheelActor.positions.indexOf(targetPos)
    if (targetPosIndex > currentPosIndex) (currentPosIndex, +1) else (currentPosIndex, -1)
  }

  private def move(
      currentPosIndex: Int,
      diff: Int,
      targetPos: String,
      intervalInMS: Int = 500,
      self: ActorRef[FilterWheelActorMessage]
  ): Behavior[FilterWheelActorMessage] = {
    if (currentPosIndex < 0 || FilterWheelActor.positions(currentPosIndex) == targetPos) return Behaviors.same

    val nextPosIndex = currentPosIndex + diff
    cswContext.timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plusMillis(intervalInMS))) {
      self ! FilterWheelActorMessage.Move(nextPosIndex, diff, targetPos, intervalInMS)
    }
    behavior(FilterWheelActor.positions(nextPosIndex))
  }

  def behavior(currentPos: String): Behavior[FilterWheelActorMessage] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case FilterWheelActorMessage.Wheel1(target) =>
          val (currentIndex, diff) = getCurrentIndexAndDiff(currentPos, target)
          move(currentIndex, diff, target, 500, ctx.self)

        case FilterWheelActorMessage.Move(currentIndex, diff, targetPos, intervalInMS) =>
          move(currentIndex, diff, targetPos, intervalInMS, ctx.self)
      }
    }
  }
}

trait FilterWheelActorMessage

object FilterWheelActorMessage {
  case class Wheel1(target: String)                                                   extends FilterWheelActorMessage
  case class Move(currentIndex: Int, diff: Int, targetPos: String, intervalInMS: Int) extends FilterWheelActorMessage
}

object FilterWheelActor {
  val positions       = List("f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9", "f10", "f11", "f12", "f13", "f14", "f15", "f16")
  val initialPosition = "f1"

  def behavior(cswContext: CswContext): Behavior[FilterWheelActorMessage] = new FilterWheelActor(cswContext).behavior(initialPosition)
}

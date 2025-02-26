package wfos.filter

import org.apache.pekko.Done
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import csw.framework.models.CswContext
import csw.params.commands.CommandIssue.AssemblyBusyIssue
import csw.params.commands.CommandResponse.{Accepted, Completed, Invalid}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import wfos.filter.events.{BlueFilterPositionEvent, FilterPositionEvent, RedFilterPositionEvent}
import wfos.filter.models.WheelCommand.MoveStep
import wfos.filter.models.{AssemblyConfiguration, BlueFilterWheelPosition, FilterWheelPosition, RedFilterWheelPosition, WheelCommand}

import scala.concurrent.Future

abstract class FilterWheelActor(cswContext: CswContext, configuration: AssemblyConfiguration) {

  private val timeServiceScheduler = cswContext.timeServiceScheduler
  private val crm                  = cswContext.commandResponseManager

  private def unhandledMessage(state: String) = s"$name: Cannot accept command: move in [$state] state"

  final def idle(current: FilterWheelPosition): Behavior[WheelCommand] =
    Behaviors.receive { (ctx, msg) =>
      val log = getLogger(ctx)
      msg match {
        case WheelCommand.Move(target, id) => moving(id, current, target)(ctx)
        case WheelCommand.IsValidMove(runId, replyTo) =>
          replyTo ! Accepted(runId)
          Behaviors.same
        case WheelCommand.MoveStep =>
          log.error(unhandledMessage("idle"))
          Behaviors.unhandled
      }
    }

  final def moving(runId: Id, current: FilterWheelPosition, target: FilterWheelPosition)(
      ctx: ActorContext[WheelCommand]
  ): Behavior[WheelCommand] = {
    val log = getLogger(ctx)
    log.info(s"$name: current position is: $current")

    if (current == target) {
      log.info(s"$name: target position: $current reached")
      publishPosition(current, target, dark = false)
      crm.updateCommand(Completed(runId))
      idle(current)
    }
    else {
      scheduleMoveStep(ctx.self)
      moveBehavior(runId, current, target)
    }
  }

  private def moveBehavior(runId: Id, currentPos: FilterWheelPosition, targetPos: FilterWheelPosition): Behavior[WheelCommand] =
    Behaviors.receive { (ctx, msg) =>
      val log = getLogger(ctx)
      msg match {
        case WheelCommand.IsValidMove(runId, replyTo) =>
          val errMsg = unhandledMessage("moving")
          log.error(errMsg)
          val issue = AssemblyBusyIssue(errMsg)
          replyTo ! Invalid(runId, issue)
          Behaviors.same
        case WheelCommand.MoveStep =>
          val nextPosition = currentPos.nextPosition(targetPos)
          if (nextPosition != targetPos) publishPosition(nextPosition, targetPos, dark = true)
          moving(runId, nextPosition, targetPos)(ctx)
        case WheelCommand.Move(_, runId) =>
          val errMsg = unhandledMessage("moving")
          log.error(errMsg)
          crm.updateCommand(Invalid(runId, AssemblyBusyIssue(errMsg)))
          Behaviors.unhandled
      }
    }

  private def scheduleMoveStep(self: ActorRef[WheelCommand]) =
    timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plus(configuration.movementDelay))) {
      self ! MoveStep
    }

  private def getLogger(ctx: ActorContext[WheelCommand]) = cswContext.loggerFactory.getLogger(ctx)

  private lazy val eventPublisher = cswContext.eventService.defaultPublisher

  val filterPositionEvent: FilterPositionEvent
  val name: String

  def publishPosition(current: FilterWheelPosition, target: FilterWheelPosition, dark: Boolean): Future[Done] = {
    eventPublisher.publish(filterPositionEvent.make(current, target, dark))
  }
}

class RedFilterWheelActor(cswContext: CswContext, configuration: AssemblyConfiguration)
    extends FilterWheelActor(cswContext, configuration) {

  override val filterPositionEvent = new RedFilterPositionEvent(cswContext.componentInfo.prefix)
  override val name: String        = "Red Filter Wheel"

  def behavior: Behavior[WheelCommand] =
    new RedFilterWheelActor(cswContext, configuration).idle(RedFilterWheelPosition.RPrime)

}

class BlueFilterWheelActor(cswContext: CswContext, configuration: AssemblyConfiguration)
    extends FilterWheelActor(cswContext, configuration) {

  override val filterPositionEvent = new BlueFilterPositionEvent(cswContext.componentInfo.prefix)
  override val name: String        = "Blue Filter Wheel"

  def behavior: Behavior[WheelCommand] =
    new BlueFilterWheelActor(cswContext, configuration).idle(BlueFilterWheelPosition.UPrime)

}

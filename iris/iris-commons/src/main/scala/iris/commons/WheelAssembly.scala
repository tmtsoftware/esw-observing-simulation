package iris.commons

import akka.Done
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import csw.framework.models.CswContext
import csw.params.commands.CommandIssue.AssemblyBusyIssue
import csw.params.commands.CommandResponse.{Accepted, Completed, Invalid}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import iris.commons.models.WheelCommand.MoveStep
import iris.commons.models.{Position, WheelCommand, AssemblyConfiguration}

import scala.concurrent.Future

abstract class WheelAssembly[B <: Position[B]](cswContext: CswContext, configuration: AssemblyConfiguration) {
  private val timeServiceScheduler = cswContext.timeServiceScheduler
  private val crm                  = cswContext.commandResponseManager

  protected val name: String
  protected def publishPosition(current: B, target: B, dark: Boolean): Future[Done]
  private def unhandledMessage(state: String) = s"$name: Cannot accept command: move in [$state] state"

  final def idle(current: B): Behavior[WheelCommand[B]] =
    Behaviors.receive { (ctx, msg) =>
      val log = getLogger(ctx)
      msg match {
        case WheelCommand.Move(target, id) => moving(id, current, target)(ctx)
        case WheelCommand.IsValidMove(runId, replyTo) =>
          replyTo ! Accepted(runId)
          Behaviors.same
        case cmd @ WheelCommand.MoveStep =>
          log.error(unhandledMessage("idle"))
          Behaviors.unhandled
      }
    }

  final def moving(runId: Id, current: B, target: B)(ctx: ActorContext[WheelCommand[B]]): Behavior[WheelCommand[B]] = {
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

  private def moveBehavior(runId: Id, currentPos: B, targetPos: B): Behavior[WheelCommand[B]] =
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

  private def scheduleMoveStep(self: ActorRef[WheelCommand[B]]) =
    timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plus(configuration.movementDelay))) {
      self ! MoveStep
    }

  private def getLogger(ctx: ActorContext[WheelCommand[B]]) = cswContext.loggerFactory.getLogger(ctx)
}

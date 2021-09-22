package iris.imagerfilter

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import csw.framework.models.CswContext
import csw.params.commands.CommandIssue.AssemblyBusyIssue
import csw.params.commands.CommandResponse.{Accepted, Completed, Invalid, ValidateCommandResponse}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import iris.imagerfilter.FilterWheelCommand.MoveStep
import iris.imagerfilter.events.ImagerPositionEvent
import iris.imagerfilter.models.FilterWheelPosition

sealed trait FilterWheelCommand

object FilterWheelCommand {
  case class MoveWheel1(target: FilterWheelPosition, runId: Id)                 extends FilterWheelCommand
  case object MoveStep                                                          extends FilterWheelCommand
  case class IsValidMove(runId: Id, replyTo: ActorRef[ValidateCommandResponse]) extends FilterWheelCommand
}

class FilterWheelActor(cswContext: CswContext, configuration: FilterWheelConfiguration) {
  private val timeServiceScheduler = cswContext.timeServiceScheduler
  private val crm                  = cswContext.commandResponseManager
  private lazy val eventPublisher  = cswContext.eventService.defaultPublisher

  def idle(current: FilterWheelPosition): Behavior[FilterWheelCommand] =
    Behaviors.receive { (ctx, msg) =>
      val log = getLogger(ctx)
      msg match {
        case FilterWheelCommand.IsValidMove(runId, replyTo) =>
          replyTo ! Accepted(runId)
          Behaviors.same
        case FilterWheelCommand.MoveWheel1(target, id) => moving(id, current, target)
        case cmd @ FilterWheelCommand.MoveStep =>
          log.error(s"Cannot accept command: $cmd in [idle] state")
          Behaviors.unhandled
      }
    }

  private def moving(runId: Id, current: FilterWheelPosition, target: FilterWheelPosition): Behavior[FilterWheelCommand] =
    Behaviors.setup { ctx =>
      val log = getLogger(ctx)
      log.info(s"Filter wheels current position is: $current")

      if (current == target) {
        log.info(s"Filter wheels target position: $current reached")
        publishPosition(current, target, dark = false)
        crm.updateCommand(Completed(runId))
        idle(current)
      }
      else {
        scheduleMoveStep(ctx.self)
        Behaviors.receiveMessage {
          case FilterWheelCommand.IsValidMove(runId, replyTo) =>
            val errMsg = s"Cannot accept command in [moving] state"
            log.error(errMsg)
            val issue = AssemblyBusyIssue(errMsg)
            replyTo ! Invalid(runId, issue)
            Behaviors.same
          case FilterWheelCommand.MoveStep =>
            val nextPosition = current.nextPosition(target)
            if (nextPosition != target) publishPosition(nextPosition, target, dark = true)
            moving(runId, nextPosition, target)
          case cmd @ FilterWheelCommand.MoveWheel1(_, runId) =>
            val errMsg = s"Cannot accept command: $cmd in [moving] state"
            log.error(errMsg)
            crm.updateCommand(Invalid(runId, AssemblyBusyIssue(errMsg)))
            Behaviors.unhandled
        }
      }
    }

  private def scheduleMoveStep(self: ActorRef[FilterWheelCommand]) =
    timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plus(configuration.wheelDelay))) {
      self ! MoveStep
    }

  private def publishPosition(current: FilterWheelPosition, target: FilterWheelPosition, dark: Boolean) =
    eventPublisher.publish(ImagerPositionEvent.make(current, target, dark))

  private def getLogger(ctx: ActorContext[FilterWheelCommand]) = cswContext.loggerFactory.getLogger(ctx)
}

object FilterWheelActor {
  val InitialPosition: FilterWheelPosition = FilterWheelPosition.F1

  def behavior(cswContext: CswContext, configuration: FilterWheelConfiguration): Behavior[FilterWheelCommand] =
    new FilterWheelActor(cswContext, configuration).idle(InitialPosition)
}

package iris.imagerfilter

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import csw.framework.models.CswContext
import csw.params.commands.CommandIssue.AssemblyBusyIssue
import csw.params.commands.CommandResponse.{Completed, Invalid}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import iris.imagerfilter.FilterWheelCommand.MoveStep
import iris.imagerfilter.models.FilterWheelPosition

sealed trait FilterWheelCommand

object FilterWheelCommand {
  case class MoveWheel1(target: FilterWheelPosition, runId: Id) extends FilterWheelCommand
  case object MoveStep                                          extends FilterWheelCommand
}

class FilterWheelActor(cswContext: CswContext, configuration: FilterWheelConfiguration) {
  private val timeServiceScheduler = cswContext.timeServiceScheduler
  private val crm                  = cswContext.commandResponseManager

  def idle(current: FilterWheelPosition): Behavior[FilterWheelCommand] =
    Behaviors.receive { (ctx, msg) =>
      val log = getLogger(ctx)
      msg match {
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
        crm.updateCommand(Completed(runId))
        idle(current)
      }
      else {
        scheduleMoveStep(ctx.self)
        Behaviors.receiveMessage {
          case FilterWheelCommand.MoveStep => moving(runId, current.nextPosition(target), target)
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
  private def getLogger(ctx: ActorContext[FilterWheelCommand]) = cswContext.loggerFactory.getLogger(ctx)
}

object FilterWheelActor {
  private val InitialPosition = FilterWheelPosition.F1

  def behavior(cswContext: CswContext, configuration: FilterWheelConfiguration): Behavior[FilterWheelCommand] =
    new FilterWheelActor(cswContext, configuration).idle(InitialPosition)
}

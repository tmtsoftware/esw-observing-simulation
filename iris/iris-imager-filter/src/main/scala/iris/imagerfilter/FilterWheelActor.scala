package iris.imagerfilter

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.framework.models.CswContext
import csw.params.commands.CommandResponse.Completed
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
  private lazy val timeServiceScheduler = cswContext.timeServiceScheduler
  private lazy val crm                  = cswContext.commandResponseManager

  def idle(current: FilterWheelPosition): Behavior[FilterWheelCommand] =
    Behaviors.receiveMessage {
      case FilterWheelCommand.MoveWheel1(target, id) => moving(id, current, target, current.step(target))
      case FilterWheelCommand.MoveStep               => Behaviors.unhandled
    }

  private def moving(runId: Id, current: FilterWheelPosition, target: FilterWheelPosition, step: Int): Behavior[FilterWheelCommand] = {
    if (current == target) {
      crm.updateCommand(Completed(runId))
      idle(current)
    }
    else {
      Behaviors.setup { ctx =>
        scheduleMoveStep(ctx.self)
        Behaviors.receiveMessage {
          case FilterWheelCommand.MoveStep         => moving(current.nextPosition(step), target, step)
          case FilterWheelCommand.MoveWheel1(_, _) => Behaviors.unhandled
        }
      }
    }
  }

  private def scheduleMoveStep(self: ActorRef[FilterWheelCommand]) =
    timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plus(configuration.wheelDelay))) {
      self ! MoveStep
    }
}

object FilterWheelActor {
  private val InitialPosition = FilterWheelPosition.F1

  def behavior(cswContext: CswContext, configuration: FilterWheelConfiguration): Behavior[FilterWheelCommand] =
    new FilterWheelActor(cswContext, configuration).idle(InitialPosition)
}

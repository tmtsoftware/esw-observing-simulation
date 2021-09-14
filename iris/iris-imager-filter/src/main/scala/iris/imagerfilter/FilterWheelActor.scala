package iris.imagerfilter

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.framework.models.CswContext
import csw.time.core.models.UTCTime
import iris.imagerfilter.models.FilterWheelPosition

sealed trait FilterWheelCommand

object FilterWheelCommand {
  case class Wheel1(target: FilterWheelPosition)      extends FilterWheelCommand
  case class MoveOneStep(target: FilterWheelPosition) extends FilterWheelCommand
}

class FilterWheelActor(cswContext: CswContext, configuration: FilterWheelConfiguration) {
  private lazy val timeServiceScheduler = cswContext.timeServiceScheduler

  def behavior(current: FilterWheelPosition): Behavior[FilterWheelCommand] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case FilterWheelCommand.Wheel1(target)      => move(current, target, ctx.self)
        case FilterWheelCommand.MoveOneStep(target) => move(current, target, ctx.self)
      }
    }
  }

  private def move(
      current: FilterWheelPosition,
      target: FilterWheelPosition,
      self: ActorRef[FilterWheelCommand]
  ): Behavior[FilterWheelCommand] = {
    def moveOneStep() =
      timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plus(configuration.wheelDelay))) {
        self ! FilterWheelCommand.MoveOneStep(target)
      }

    if (current == target) Behaviors.same
    else {
      moveOneStep()
      behavior(current.nextPosition(target))
    }
  }
}

object FilterWheelActor {
  private val InitialPosition = FilterWheelPosition.F1

  def behavior(cswContext: CswContext, configuration: FilterWheelConfiguration): Behavior[FilterWheelCommand] =
    new FilterWheelActor(cswContext, configuration).behavior(InitialPosition)
}

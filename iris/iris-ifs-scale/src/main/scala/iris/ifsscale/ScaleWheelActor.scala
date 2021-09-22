package iris.ifsscale

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import csw.framework.models.CswContext
import csw.params.commands.CommandIssue.AssemblyBusyIssue
import csw.params.commands.CommandResponse.{Accepted, Completed, Invalid, ValidateCommandResponse}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import iris.ifsscale.events.IfsScaleEvent
import iris.ifsscale.models.ScaleLevel

sealed trait ScaleWheelCommand

object ScaleWheelCommand {
  case class UpdateScale(target: ScaleLevel, runId: Id)                         extends ScaleWheelCommand
  case class IsValidMove(runId: Id, replyTo: ActorRef[ValidateCommandResponse]) extends ScaleWheelCommand
}

class ScaleWheelActor(cswContext: CswContext, configuration: ScaleWheelConfiguration) {
  private val timeServiceScheduler = cswContext.timeServiceScheduler
  private val crm                  = cswContext.commandResponseManager
  private lazy val eventPublisher  = cswContext.eventService.defaultPublisher

  def idle(current: ScaleLevel): Behavior[ScaleWheelCommand] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case ScaleWheelCommand.IsValidMove(runId, replyTo) =>
          replyTo ! Accepted(runId)
          Behaviors.same
        case ScaleWheelCommand.UpdateScale(target, id) => scaling(id, current, target)
      }
    }

  private def scaling(runId: Id, current: ScaleLevel, target: ScaleLevel): Behavior[ScaleWheelCommand] =
    Behaviors.setup { ctx =>
      val log = getLogger(ctx)
      log.info(s"Scale wheels current position is: $current")

      if (current != target) {
        scheduleMoveStep(ctx.self, target, runId)
      }
      else {
        ctx.self ! ScaleWheelCommand.UpdateScale(target, runId)
      }

      Behaviors.receiveMessage {
        case ScaleWheelCommand.IsValidMove(runId, replyTo) =>
          val errMsg = s"Cannot accept command in [scaling] state"
          log.error(errMsg)
          val issue = AssemblyBusyIssue(errMsg)
          replyTo ! Invalid(runId, issue)
          crm.updateCommand(Invalid(runId, AssemblyBusyIssue(errMsg)))
          Behaviors.same
        case ScaleWheelCommand.UpdateScale(target, runId) =>
          log.info(s"Scale wheels updated to: $target")
          publishPosition(target, target)
          crm.updateCommand(Completed(runId))
          idle(target)
      }
    }

  private def scheduleMoveStep(self: ActorRef[ScaleWheelCommand], target: ScaleLevel, runId: Id) =
    timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plus(configuration.wheelDelay))) {
      self ! ScaleWheelCommand.UpdateScale(target, runId)
    }

  private def publishPosition(current: ScaleLevel, target: ScaleLevel) =
    eventPublisher.publish(IfsScaleEvent.make(current, target))

  private def getLogger(ctx: ActorContext[ScaleWheelCommand]) = cswContext.loggerFactory.getLogger(ctx)
}

object ScaleWheelActor {
  val InitialScale: ScaleLevel = ScaleLevel.S25

  def behavior(cswContext: CswContext, configuration: ScaleWheelConfiguration): Behavior[ScaleWheelCommand] =
    new ScaleWheelActor(cswContext, configuration).idle(InitialScale)
}

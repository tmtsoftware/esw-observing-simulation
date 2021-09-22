package iris.ifsres

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import csw.framework.models.CswContext
import csw.params.commands.CommandIssue.AssemblyBusyIssue
import csw.params.commands.CommandResponse.{Accepted, Completed, Invalid, ValidateCommandResponse}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import iris.ifsres.ResWheelCommand.MoveStep
import iris.ifsres.events.IfsPositionEvent
import iris.ifsres.models.ResWheelPosition

sealed trait ResWheelCommand

object ResWheelCommand {
  case class MoveSpectralResolution(target: ResWheelPosition, runId: Id)        extends ResWheelCommand
  case object MoveStep                                                          extends ResWheelCommand
  case class IsValidMove(runId: Id, replyTo: ActorRef[ValidateCommandResponse]) extends ResWheelCommand
}

class ResWheelActor(cswContext: CswContext, configuration: ResWheelConfiguration) {
  private val timeServiceScheduler = cswContext.timeServiceScheduler
  private val crm                  = cswContext.commandResponseManager
  private lazy val eventPublisher  = cswContext.eventService.defaultPublisher

  def idle(current: ResWheelPosition): Behavior[ResWheelCommand] =
    Behaviors.receive { (ctx, msg) =>
      val log = getLogger(ctx)
      msg match {
        case ResWheelCommand.IsValidMove(runId, replyTo) =>
          replyTo ! Accepted(runId)
          Behaviors.same
        case ResWheelCommand.MoveSpectralResolution(target, id) => moving(id, current, target)
        case cmd @ ResWheelCommand.MoveStep =>
          log.error(s"Cannot accept command: $cmd in [idle] state")
          Behaviors.unhandled
      }
    }

  private def moving(runId: Id, current: ResWheelPosition, target: ResWheelPosition): Behavior[ResWheelCommand] =
    Behaviors.setup { ctx =>
      val log = getLogger(ctx)
      log.info(s"Res wheels current position is: $current")

      if (current == target) {
        log.info(s"Res wheels target position: $current reached")
        publishPosition(current, target, dark = false)
        crm.updateCommand(Completed(runId))
        idle(current)
      }
      else {
        scheduleMoveStep(ctx.self)
        Behaviors.receiveMessage {
          case ResWheelCommand.IsValidMove(runId, replyTo) =>
            val errMsg = s"Cannot accept command in [moving] state"
            log.error(errMsg)
            val issue = AssemblyBusyIssue(errMsg)
            replyTo ! Invalid(runId, issue)
            crm.updateCommand(Invalid(runId, AssemblyBusyIssue(errMsg)))
            Behaviors.same
          case ResWheelCommand.MoveStep =>
            val nextPosition = current.nextPosition(target)
            if (nextPosition != target) publishPosition(nextPosition, target, dark = true)
            moving(runId, nextPosition, target)
        }
      }
    }

  private def scheduleMoveStep(self: ActorRef[ResWheelCommand]) =
    timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plus(configuration.wheelDelay))) {
      self ! MoveStep
    }

  private def publishPosition(current: ResWheelPosition, target: ResWheelPosition, dark: Boolean) =
    eventPublisher.publish(IfsPositionEvent.make(current, target))

  private def getLogger(ctx: ActorContext[ResWheelCommand]) = cswContext.loggerFactory.getLogger(ctx)
}

object ResWheelActor {
  val InitialPosition: ResWheelPosition = ResWheelPosition.R4000_Z

  def behavior(cswContext: CswContext, configuration: ResWheelConfiguration): Behavior[ResWheelCommand] =
    new ResWheelActor(cswContext, configuration).idle(InitialPosition)
}

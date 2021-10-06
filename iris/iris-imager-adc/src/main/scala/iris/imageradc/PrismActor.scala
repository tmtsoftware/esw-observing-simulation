package iris.imageradc

import akka.actor.Cancellable
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.framework.models.CswContext
import csw.params.commands.CommandIssue.UnsupportedCommandIssue
import csw.params.commands.CommandResponse.{Accepted, Completed, Invalid}
import csw.params.events.SystemEvent
import iris.imageradc.commands.{ADCCommand, PrismCommands}
import iris.imageradc.events.{PrismCurrentEvent, PrismRetractEvent, PrismStateEvent, PrismTargetEvent}
import iris.imageradc.models.PrismState.MOVING
import iris.imageradc.models.{PrismPosition, PrismState}

import scala.compat.java8.DurationConverters.FiniteDurationops
import scala.concurrent.duration.DurationInt

class PrismActor(cswContext: CswContext) {
  var prismTarget: Double          = 0.0
  var prismCurrent: Double         = 0.0
  private val timeServiceScheduler = cswContext.timeServiceScheduler
  private val crm                  = cswContext.commandResponseManager
  private lazy val eventPublisher  = cswContext.eventService.defaultPublisher

  def inAndStopped: Behavior[PrismCommands] = {
    publishPrismState(PrismState.STOPPED)
    publishRetractPosition(PrismPosition.IN)
    Behaviors.receive { (_, msg) =>
      {
        msg match {
          case PrismCommands.RetractSelect(runId, position) =>
            position match {
              case PrismPosition.IN =>
                crm.updateCommand(Completed(runId))
                Behaviors.same
              case PrismPosition.OUT =>
                //todo add scheduler to go to out behaviour after some delay
                outAndStopped
            }
          case PrismCommands.IsValid(runId, _, replyTo) =>
            replyTo ! Accepted(runId)
            Behaviors.same
          case PrismCommands.PRISM_FOLLOW(_, targetAngle) =>
            prismTarget = targetAngle
            inAndMoving
          case PrismCommands.PRISM_STOP(_) =>
            Behaviors.same
        }
      }
    }
  }
  def outAndStopped: Behavior[PrismCommands] = {
    publishRetractPosition(PrismPosition.OUT)
    Behaviors.receive { (ctx, msg) =>
      {
        val log = cswContext.loggerFactory.getLogger(ctx)
        msg match {
          case PrismCommands.RetractSelect(runId, position) =>
            position match {
              case PrismPosition.IN =>
                //todo add scheduler to go to out behaviour after some delay
                println("going from OUT to IN")
                crm.updateCommand(Completed(runId))
                inAndStopped
              case PrismPosition.OUT =>
                crm.updateCommand(Completed(runId))
                Behaviors.same
            }
          case PrismCommands.IsValid(runId, command, replyTo) =>
            command.commandName match {
              case ADCCommand.RetractSelect =>
                replyTo ! Accepted(runId)
                Behaviors.same
              case _ =>
                val errMsg = s"Setup command: $name is not valid in disabled state."
                log.error(errMsg)
                replyTo ! Invalid(runId, UnsupportedCommandIssue(errMsg))
                Behaviors.unhandled
            }
          case PrismCommands.PRISM_FOLLOW(_, _) =>
            val errMsg = s"Setup command: $name is not valid in disabled state."
            log.error(errMsg)
            Behaviors.unhandled
          case PrismCommands.PRISM_STOP(_) =>
            val errMsg = s"Setup command: $name is not valid in disabled state."
            log.error(errMsg)
            Behaviors.unhandled
        }
      }
    }
  }
  def inAndMoving: Behavior[PrismCommands] = {
    val targetModifier         = scheduleJobForTarget
    val targetFollower         = scheduleJobForCurrent
    val publisherSubscriptions = publishPrismStatesFor(MOVING)
    Behaviors.receive { (ctx, msg) =>
      val log = cswContext.loggerFactory.getLogger(ctx)
      msg match {
        case PrismCommands.RetractSelect(_, _) =>
          val errMsg = s"Setup command: $name is not valid in moving state."
          log.error(errMsg)
          Behaviors.unhandled
        case PrismCommands.IsValid(runId, command, replyTo) =>
          command.commandName match {
            case ADCCommand.RetractSelect =>
              val errMsg = s"Setup command: $name is not valid in moving state."
              log.error(errMsg)
              replyTo ! Invalid(runId, UnsupportedCommandIssue(errMsg))
              Behaviors.unhandled
            case _ =>
              replyTo ! Accepted(runId)
              Behaviors.same
          }
        case PrismCommands.PRISM_FOLLOW(_, _) =>
          Behaviors.same
        case PrismCommands.PRISM_STOP(_) =>
          targetModifier.cancel()
          targetFollower.cancel()
          publisherSubscriptions.foreach(_.cancel())
          inAndStopped
      }
    }
  }

  private def publishPrismStatesFor(state: PrismState): List[Cancellable] = {
    def generatePrismCurrent: Option[SystemEvent] = Option {
      PrismCurrentEvent.make(prismCurrent, prismTarget - prismCurrent)
    }
    def generatePrismTarget: Option[SystemEvent] = Option {
      PrismTargetEvent.make(prismTarget)
    }

    List(
      eventPublisher.publish(generatePrismCurrent, 1.second),
      eventPublisher.publish(generatePrismTarget, 1.second),
      eventPublisher.publish(Some(getPrismStateEvent(state)), 1.second)
    )
  }

  private def publishPrismState(state: PrismState) = eventPublisher.publish(getPrismStateEvent(state))

  private def getPrismStateEvent(state: PrismState) =
    PrismStateEvent.make(state, Math.abs(prismCurrent - prismTarget) < 0.5)

  private def publishRetractPosition(position: PrismPosition): Unit = {
    eventPublisher.publish(PrismRetractEvent.make(position))
  }

  private def scheduleJobForTarget =
    timeServiceScheduler.schedulePeriodically(1.seconds.toJava) {
      if (Math.abs(prismCurrent - prismTarget) < 0.5) {
        prismTarget += 0.1
      }
    }

  private def scheduleJobForCurrent =
    timeServiceScheduler.schedulePeriodically(1.seconds.toJava) {
      prismCurrent += (prismTarget - prismCurrent) / 3
    }

  protected val name: String = "Imager ADC"
}

object PrismActor {

  def behavior(cswContext: CswContext): Behavior[PrismCommands] =
    new PrismActor(cswContext).outAndStopped

}

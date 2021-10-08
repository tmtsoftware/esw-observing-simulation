package iris.imageradc

import akka.actor.Cancellable
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
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
import scala.math.BigDecimal.RoundingMode

class PrismActor(cswContext: CswContext) {
  var prismTarget: BigDecimal      = 0.0
  var prismCurrent: BigDecimal     = 0.0
  var fastMoving: Boolean          = false
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
          case _ => Behaviors.unhandled
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
          case _ =>
            val errMsg = s"Setup command: $name is not valid in disabled state."
            log.error(errMsg)
            Behaviors.unhandled
        }
      }
    }
  }
  def inAndMoving: Behavior[PrismCommands] = {
    Behaviors.setup { ctx =>
      println("in here going to fast state >>>>>>>>>>>>>>>>>>>>>")
      fastMoving = true
      val fastMovement: BigDecimal = truncateTo1Decimal((prismTarget - prismCurrent) / 3)
      val targetModifier           = scheduleJobForPrismMovement(ctx)
      val publisherSubscriptions   = publishPrismStatesFor(MOVING)
      Behaviors.receiveMessage { msg =>
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
            //schedule ??
            Behaviors.same
          case PrismCommands.PRISM_STOP(_) =>
            targetModifier.cancel()
            publisherSubscriptions.foreach(_.cancel())
            inAndStopped
          case PrismCommands.MOVE_CURRENT =>
//            println(s"==============MOVE CURRENT===================== $fastMoving")
            if (fastMoving)
              prismCurrent += fastMovement
            else
              prismCurrent += slowMovement
            Behaviors.same
          case PrismCommands.MOVE_TARGET =>
//            println(s"---------------MOVE_TARGET------------------:${getCurrentDiff.abs.compare(0.5)}")
            if (isWithinToleranceRange) {
              prismTarget += 0.1
              fastMoving = false
            }
            ctx.self ! PrismCommands.MOVE_CURRENT
            Behaviors.same
        }
      }
    }

  }

  private def slowMovement = if (prismTarget > prismCurrent) +0.1 else -0.1

  private def isWithinToleranceRange = getCurrentDiff.abs.compare(0.5) == -1

  private def publishPrismStatesFor(state: PrismState): List[Cancellable] = {
    def generatePrismCurrent: Option[SystemEvent] = Option {
      println(s"============= CURRENT EVENT===================== ${prismCurrent.toDouble}, ${getCurrentDiff.toDouble}")
      PrismCurrentEvent.make(prismCurrent.toDouble, getCurrentDiff.toDouble)
    }
    def generatePrismTarget: Option[SystemEvent] = Option {
      println(s"============= TARGET EVENT===================== ${prismTarget.toDouble}")
      PrismTargetEvent.make(prismTarget.toDouble)
    }

    List(
      eventPublisher.publish(generatePrismCurrent, 1.second),
      eventPublisher.publish(generatePrismTarget, 1.second),
      eventPublisher.publish(Some(getPrismStateEvent(state)), 1.second)
    )
  }

  private def publishPrismState(state: PrismState) = eventPublisher.publish(getPrismStateEvent(state))

  private def getPrismStateEvent(state: PrismState) =
    PrismStateEvent.make(state, isWithinToleranceRange)

  private def publishRetractPosition(position: PrismPosition): Unit = {
    eventPublisher.publish(PrismRetractEvent.make(position))
  }

  private def scheduleJobForPrismMovement(ctx: ActorContext[PrismCommands]) = {
    timeServiceScheduler.schedulePeriodically(1.seconds.toJava) {
      ctx.self ! PrismCommands.MOVE_TARGET
    }
  }
  private def truncateTo1Decimal(value: BigDecimal) = BigDecimal(value.toDouble).setScale(1, RoundingMode.DOWN)

  private def getCurrentDiff = prismCurrent - prismTarget

  protected val name: String = "Imager ADC"
}

object PrismActor {

  def behavior(cswContext: CswContext): Behavior[PrismCommands] =
    new PrismActor(cswContext).outAndStopped

}

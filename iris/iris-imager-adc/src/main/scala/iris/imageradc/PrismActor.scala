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
      val targetFollower         = scheduleJobForCurrent(ctx)
      val targetModifier         = scheduleJobForTarget(ctx)
      val publisherSubscriptions = publishPrismStatesFor(MOVING)
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
            targetFollower.cancel()
            targetModifier.cancel()
            publisherSubscriptions.foreach(_.cancel())
            inAndStopped
          case PrismCommands.MOVE_CURRENT(moveBy: BigDecimal) =>
//            println(s"==============MOVE CURRENT===================== $fastMoving")
            if (fastMoving)
              prismCurrent += moveBy
            else
              prismCurrent = if (prismTarget > prismCurrent) prismCurrent + 0.1 else prismCurrent - 0.1
            Behaviors.same
          case PrismCommands.MOVE_TARGET =>
//            println(s"---------------MOVE_TARGET------------------${getCurrentDiff.abs.compare(0.5)}")
            if (getCurrentDiff.abs.compare(0.5) == -1) {
              prismTarget += 0.1
              fastMoving = false
            }
            Behaviors.same
        }
      }
    }

  }

  private def publishPrismStatesFor(state: PrismState): List[Cancellable] = {
    def generatePrismCurrent: Option[SystemEvent] = Option {
      PrismCurrentEvent.make(prismCurrent.toDouble, getCurrentDiff.toDouble)
    }
    def generatePrismTarget: Option[SystemEvent] = Option {
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
    PrismStateEvent.make(state, getCurrentDiff.abs.compare(0.5) == -1)

  private def publishRetractPosition(position: PrismPosition): Unit = {
    eventPublisher.publish(PrismRetractEvent.make(position))
  }

  private def scheduleJobForTarget(ctx: ActorContext[PrismCommands]) =
    timeServiceScheduler.schedulePeriodically(1.seconds.toJava) {
      ctx.self ! PrismCommands.MOVE_TARGET
    }

  private def truncateTo1Decimal(value: BigDecimal) = BigDecimal(value.toDouble).setScale(1, RoundingMode.DOWN)

  private def getCurrentDiff = prismCurrent - prismTarget

  private def scheduleJobForCurrent(ctx: ActorContext[PrismCommands]) = {
    println("in here going to fast state>>>>>>>>>>>>>>>>>>>>>")
    fastMoving = true
    val fastMove: BigDecimal = truncateTo1Decimal((prismTarget - prismCurrent) / 3)
    timeServiceScheduler.schedulePeriodically(1.seconds.toJava) {
      ctx.self ! PrismCommands.MOVE_CURRENT(fastMove)
    }
  }

  protected val name: String = "Imager ADC"
}

object PrismActor {

  def behavior(cswContext: CswContext): Behavior[PrismCommands] =
    new PrismActor(cswContext).outAndStopped

}

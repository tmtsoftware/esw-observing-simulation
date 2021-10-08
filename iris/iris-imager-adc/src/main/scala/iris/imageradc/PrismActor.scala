package iris.imageradc

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import csw.framework.models.CswContext
import csw.params.commands.CommandIssue.UnsupportedCommandIssue
import csw.params.commands.CommandResponse.{Accepted, Completed, Invalid}
import csw.params.events.SystemEvent
import csw.time.core.models.UTCTime
import iris.imageradc.commands.PrismCommands.{GOING_IN, GOING_OUT}
import iris.imageradc.commands.{ADCCommand, PrismCommands}
import iris.imageradc.events.{PrismCurrentEvent, PrismRetractEvent, PrismStateEvent, PrismTargetEvent}
import iris.imageradc.models.PrismState.MOVING
import iris.imageradc.models.{AssemblyConfiguration, PrismPosition, PrismState}

import scala.math.BigDecimal.RoundingMode

class PrismActor(cswContext: CswContext, adcImagerConfiguration: AssemblyConfiguration) {
  var prismTarget: BigDecimal      = 0.0
  var prismCurrent: BigDecimal     = 0.0
  var fastMoving: Boolean          = false
  private val timeServiceScheduler = cswContext.timeServiceScheduler
  private val crm                  = cswContext.commandResponseManager
  private lazy val eventPublisher  = cswContext.eventService.defaultPublisher

  def inAndStopped: Behavior[PrismCommands] = {
    publishPrismState(PrismState.STOPPED)
    publishRetractPosition(PrismPosition.IN)
    Behaviors.receive { (ctx, msg) =>
      {
        msg match {
          case PrismCommands.RETRACT_SELECT(runId, position) =>
            position match {
              case PrismPosition.IN =>
                crm.updateCommand(Completed(runId))
                Behaviors.same
              case PrismPosition.OUT =>
                println("RECEIVED PrismPosition.OUT")
                timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plus(adcImagerConfiguration.retractSelectDelay))) {
                  crm.updateCommand(Completed(runId))
                  ctx.self ! GOING_OUT
                }
                Behaviors.same
            }
          case PrismCommands.IS_VALID(runId, _, replyTo) =>
            replyTo ! Accepted(runId)
            Behaviors.same
          case PrismCommands.PRISM_FOLLOW(_, targetAngle) =>
            prismTarget = truncateTo1DecimalAndNormalizeToCompleteAngle(targetAngle)
            inAndMoving
          case PrismCommands.PRISM_STOP(_) =>
            Behaviors.same
          case GOING_OUT =>
            println("GOING_OUT")
            outAndStopped
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
          case PrismCommands.RETRACT_SELECT(runId, position) =>
            position match {
              case PrismPosition.IN =>
                println("RECEIVED PrismPosition.IN")
                timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plus(adcImagerConfiguration.retractSelectDelay))) {
                  crm.updateCommand(Completed(runId))
                  ctx.self ! GOING_IN
                }
                Behaviors.same
              case PrismPosition.OUT =>
                crm.updateCommand(Completed(runId))
                Behaviors.same
            }
          case PrismCommands.IS_VALID(runId, command, replyTo) =>
            command.commandName match {
              case ADCCommand.RetractSelect =>
                replyTo ! Accepted(runId)
                Behaviors.same
              case cmd =>
                val errMsg = s"Setup command: ${cmd.name} is not valid in disabled state."
                log.error(errMsg)
                replyTo ! Invalid(runId, UnsupportedCommandIssue(errMsg))
                Behaviors.unhandled
            }
          case GOING_IN =>
            println("GOING_IN")
            inAndStopped
          case cmd =>
            val errMsg = s"$cmd is not valid in disabled state."
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
      val fastMovement: BigDecimal = truncateTo1DecimalAndNormalizeToCompleteAngle((prismTarget - prismCurrent) / 3)
      val targetModifier           = scheduleJobForPrismMovement(ctx)
      Behaviors.receiveMessage { msg =>
        val log = cswContext.loggerFactory.getLogger(ctx)
        msg match {
          case cmd @ PrismCommands.RETRACT_SELECT(_, _) =>
            val errMsg = s"$cmd is not valid in moving state."
            log.error(errMsg)
            Behaviors.unhandled
          case PrismCommands.IS_VALID(runId, command, replyTo) =>
            command.commandName match {
              case ADCCommand.RetractSelect =>
                val errMsg = s"Setup command: ${command.commandName.name} is not valid in moving state."
                log.error(errMsg)
                replyTo ! Invalid(runId, UnsupportedCommandIssue(errMsg))
                Behaviors.unhandled
              case _ =>
                replyTo ! Accepted(runId)
                Behaviors.same
            }
          case cmd @ PrismCommands.PRISM_FOLLOW(_, _) =>
            //TODO ask if required, schedule ??
            Behaviors.same
          case PrismCommands.PRISM_STOP(_) =>
            targetModifier.cancel()
            inAndStopped
          case PrismCommands.MOVE_CURRENT =>
            println(s"==============MOVE CURRENT===================== $fastMoving")
            if (fastMoving)
              prismCurrent = truncateTo1DecimalAndNormalizeToCompleteAngle(prismCurrent + fastMovement)
            else
              prismCurrent = truncateTo1DecimalAndNormalizeToCompleteAngle(prismCurrent + slowMovement)
            publishEvent(PrismCurrentEvent.make(prismCurrent.toDouble, getCurrentDiff.toDouble))
            Behaviors.same
          case PrismCommands.MOVE_TARGET =>
            println(s"---------------MOVE_TARGET------------------:${getCurrentDiff.abs.compare(0.5)}")
            if (isWithinToleranceRange) {
              prismTarget = truncateTo1DecimalAndNormalizeToCompleteAngle(prismTarget + adcImagerConfiguration.targetMovementAngle)
              fastMoving = false
            }
            ctx.self ! PrismCommands.MOVE_CURRENT
            publishEvent(PrismTargetEvent.make(prismTarget.toDouble))
            publishPrismState(MOVING)
            Behaviors.same
        }
      }
    }

  }

  private def slowMovement =
    if (prismTarget > prismCurrent) +adcImagerConfiguration.targetMovementAngle else -adcImagerConfiguration.targetMovementAngle

  private def isWithinToleranceRange = getCurrentDiff.abs.compare(0.5) == -1

  private def publishEvent(event: SystemEvent) = eventPublisher.publish(event)

  private def publishPrismState(state: PrismState) = eventPublisher.publish(getPrismStateEvent(state))

  private def getPrismStateEvent(state: PrismState) =
    PrismStateEvent.make(state, isWithinToleranceRange)

  private def publishRetractPosition(position: PrismPosition): Unit = {
    eventPublisher.publish(PrismRetractEvent.make(position))
  }

  private def scheduleJobForPrismMovement(ctx: ActorContext[PrismCommands]) = {
    timeServiceScheduler.schedulePeriodically(adcImagerConfiguration.targetMovementDelay) {
      ctx.self ! PrismCommands.MOVE_TARGET
    }
  }
  private def truncateTo1DecimalAndNormalizeToCompleteAngle(value: BigDecimal): BigDecimal =
    BigDecimal(value.toDouble % 360).setScale(1, RoundingMode.DOWN)

  private def getCurrentDiff = prismCurrent - prismTarget
}

object PrismActor {

  def behavior(cswContext: CswContext, adcImagerConfiguration: AssemblyConfiguration): Behavior[PrismCommands] =
    new PrismActor(cswContext, adcImagerConfiguration).outAndStopped

}

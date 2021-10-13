package iris.imageradc

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import csw.framework.models.CswContext
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandIssue.UnsupportedCommandIssue
import csw.params.commands.CommandResponse.{Accepted, Completed, Invalid, ValidateCommandResponse}
import csw.params.commands.Setup
import csw.params.core.models.Id
import csw.params.events.SystemEvent
import csw.time.core.models.UTCTime
import iris.imageradc.commands.PrismCommands.{GoingIn, GoingOut}
import iris.imageradc.commands.{ADCCommand, PrismCommands}
import iris.imageradc.events.{PrismCurrentEvent, PrismRetractEvent, PrismStateEvent, PrismTargetEvent}
import iris.imageradc.models.PrismState.MOVING
import iris.imageradc.models.{AssemblyConfiguration, PrismPosition, PrismState}

import scala.math.BigDecimal.RoundingMode

class PrismActor(cswContext: CswContext, adcImagerConfiguration: AssemblyConfiguration, logger: Logger) {
  private var prismTarget: BigDecimal  = 0.0
  private var prismCurrent: BigDecimal = 0.0
  private var fastMoving: Boolean      = false
  private val timeServiceScheduler     = cswContext.timeServiceScheduler
  private val crm                      = cswContext.commandResponseManager
  private lazy val eventPublisher      = cswContext.eventService.defaultPublisher

  def inAndStopped: Behavior[PrismCommands] = {
    publishPrismState(PrismState.STOPPED)
    publishRetractPosition(PrismPosition.IN)
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case PrismCommands.RetractSelect(runId, position) =>
          position match {
            case PrismPosition.IN =>
              crm.updateCommand(Completed(runId))
              Behaviors.same
            case PrismPosition.OUT =>
              startRetracting(runId)(ctx.self ! GoingOut)
              goingOut
          }
        case PrismCommands.IsValid(runId, command, replyTo) =>
          isValid(predicate = true)("Retracting IN", runId, command, replyTo)
        case PrismCommands.PrismFollow(_, targetAngle) =>
          prismTarget = truncateTo1DecimalAndNormalizeToCompleteAngle(targetAngle)
          inAndMoving
        case PrismCommands.PrismStop(_) =>
          Behaviors.same
        case _ => Behaviors.unhandled
      }
    }
  }

  def goingIn: Behavior[PrismCommands] =
    Behaviors.receiveMessage {
      case PrismCommands.GoingIn =>
        inAndStopped
      case PrismCommands.IsValid(runId, command, replyTo) =>
        isValid(predicate = false)("Retracting IN", runId, command, replyTo)
      case cmd =>
        val errMsg = s"$cmd is not valid in Retracting IN state."
        logger.error(errMsg)
        Behaviors.unhandled
    }

  def goingOut: Behavior[PrismCommands] =
    Behaviors.receiveMessage {
      case PrismCommands.GoingOut =>
        outAndStopped
      case PrismCommands.IsValid(runId, command, replyTo) =>
        isValid(predicate = false)("Retracting OUT", runId, command, replyTo)
      case cmd =>
        val errMsg = s"$cmd is not valid in Retracting OUT state."
        logger.error(errMsg)
        Behaviors.unhandled
    }

  def outAndStopped: Behavior[PrismCommands] = {
    publishRetractPosition(PrismPosition.OUT)
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case PrismCommands.RetractSelect(runId, position) =>
          position match {
            case PrismPosition.IN =>
              startRetracting(runId)(ctx.self ! GoingIn)
              goingIn
            case PrismPosition.OUT =>
              crm.updateCommand(Completed(runId))
              Behaviors.same
          }
        case PrismCommands.IsValid(runId, command, replyTo) =>
          isValid(command.commandName == ADCCommand.RetractSelect)("disabled", runId, command, replyTo)
        case cmd =>
          val errMsg = s"$cmd is not valid in disabled state."
          logger.error(errMsg)
          Behaviors.unhandled
      }
    }
  }

  def inAndMoving: Behavior[PrismCommands] =
    Behaviors.setup { ctx =>
      fastMoving = true
      val fastMovement: BigDecimal = truncateTo1DecimalAndNormalizeToCompleteAngle((prismTarget - prismCurrent) / 3)
      val targetModifier           = scheduleJobForPrismMovement(ctx)
      Behaviors.receiveMessage {
        case cmd @ PrismCommands.RetractSelect(_, _) =>
          val errMsg = s"$cmd is not valid in moving state."
          logger.error(errMsg)
          Behaviors.unhandled
        case PrismCommands.IsValid(runId, command, replyTo) =>
          isValid(command.commandName != ADCCommand.RetractSelect)("moving", runId, command, replyTo)
        case cmd @ PrismCommands.PrismFollow(_, _) =>
          //TODO ask if required, schedule ??
          Behaviors.same
        case PrismCommands.PrismStop(_) =>
          targetModifier.cancel()
          inAndStopped
        case PrismCommands.MoveCurrent =>
          if (fastMoving)
            prismCurrent = truncateTo1DecimalAndNormalizeToCompleteAngle(prismCurrent + fastMovement)
          else
            prismCurrent = truncateTo1DecimalAndNormalizeToCompleteAngle(prismCurrent + slowMovement)
          publishEvent(PrismCurrentEvent.make(prismCurrent.toDouble, getCurrentDiff.toDouble))
          Behaviors.same
        case PrismCommands.MoveTarget =>
          if (isWithinToleranceRange) {
            prismTarget = truncateTo1DecimalAndNormalizeToCompleteAngle(prismTarget + adcImagerConfiguration.targetMovementAngle)
            fastMoving = false
          }
          ctx.self ! PrismCommands.MoveCurrent
          publishEvent(PrismTargetEvent.make(prismTarget.toDouble))
          publishPrismState(MOVING)
          Behaviors.same
      }
    }

  private def isValid(
      predicate: Boolean
  )(state: String, runId: Id, command: Setup, replyTo: ActorRef[ValidateCommandResponse]): Behavior[PrismCommands] = {
    if (predicate) replyTo ! Accepted(runId)
    else {
      val errMsg = s"Setup command: ${command.commandName.name} is not valid in $state state."
      logger.error(errMsg)
      replyTo ! Invalid(runId, UnsupportedCommandIssue(errMsg))
    }
    Behaviors.same
  }

  private def startRetracting(runId: Id)(whenDone: => Unit) =
    timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plus(adcImagerConfiguration.retractSelectDelay))) {
      crm.updateCommand(Completed(runId))
      whenDone
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
      ctx.self ! PrismCommands.MoveTarget
    }
  }
  private def truncateTo1DecimalAndNormalizeToCompleteAngle(value: BigDecimal): BigDecimal =
    BigDecimal(value.toDouble % 360).setScale(1, RoundingMode.DOWN)

  private def getCurrentDiff = prismTarget - prismCurrent
}

object PrismActor {

  def behavior(cswContext: CswContext, adcImagerConfiguration: AssemblyConfiguration, logger: Logger): Behavior[PrismCommands] =
    new PrismActor(cswContext, adcImagerConfiguration, logger: Logger).outAndStopped

}

package iris.imageradc

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.framework.models.CswContext
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandIssue.UnsupportedCommandIssue
import csw.params.commands.CommandResponse.{Accepted, Completed, Invalid}
import csw.params.core.models.Id
import csw.params.events.SystemEvent
import csw.time.core.models.UTCTime
import csw.time.scheduler.api.Cancellable
import iris.imageradc.commands.PrismCommands.{GoingIn, GoingOut}
import iris.imageradc.commands.{ADCCommand, PrismCommands}
import iris.imageradc.events.{PrismCurrentEvent, PrismRetractEvent, PrismStateEvent}
import iris.imageradc.models.PrismState.MOVING
import iris.imageradc.models.{AssemblyConfiguration, PrismAngle, PrismPosition, PrismState}

class PrismActor(cswContext: CswContext, adcImagerConfiguration: AssemblyConfiguration, logger: Logger) {
  private val targetVelocity: Double = adcImagerConfiguration.targetMovementAngle
  private val tolerance: BigDecimal  = adcImagerConfiguration.toleranceAngle
  private val prismAngle             = new PrismAngle(0.0, BigDecimal(targetVelocity), BigDecimal(targetVelocity), tolerance)
  private val timeServiceScheduler   = cswContext.timeServiceScheduler
  private val crm                    = cswContext.commandResponseManager
  private lazy val eventPublisher    = cswContext.eventService.defaultPublisher

  def setup: Behavior[PrismCommands] = Behaviors.setup(ctx => outAndStopped(ctx.self))

  private def inAndStopped(self: ActorRef[PrismCommands]): Behavior[PrismCommands] = {
    logger.info("Prism is now in Retracted IN position")
    publishPrismState(PrismState.STOPPED)
    publishRetractPosition(PrismPosition.IN)
    //start subscription
    receiveWithDefaultBehavior("IN") {
      case PrismCommands.RetractSelect(runId, position) =>
        position match {
          case PrismPosition.IN =>
            crm.updateCommand(Completed(runId))
            Behaviors.same
          case PrismPosition.OUT =>
            //stop subscription
            startRetracting(runId)(self ! GoingOut)
            goingOut(self)
        }
      case PrismCommands.IsValid(runId, _, replyTo) =>
        replyTo ! Accepted(runId)
        Behaviors.same
      case PrismCommands.PrismFollow(targetAngle) =>
        prismAngle.setTarget(targetAngle)
        inAndMoving(self)
    }
  }

  private def goingIn(self: ActorRef[PrismCommands]): Behavior[PrismCommands] =
    receiveWithDefaultBehavior("Retracting IN") { case PrismCommands.GoingIn =>
      inAndStopped(self)
    }

  private def goingOut(self: ActorRef[PrismCommands]): Behavior[PrismCommands] =
    receiveWithDefaultBehavior("Retracting OUT") { case PrismCommands.GoingOut =>
      outAndStopped(self)
    }

  private def outAndStopped(self: ActorRef[PrismCommands]): Behavior[PrismCommands] = {
    logger.info("Prism is now in Retracted OUT position")
    publishRetractPosition(PrismPosition.OUT)
    // stop subscription
    receiveWithDefaultBehavior("OUT") {
      case PrismCommands.RetractSelect(runId, position) =>
        position match {
          case PrismPosition.IN =>
            startRetracting(runId)(self ! GoingIn)
            goingIn(self)
          case PrismPosition.OUT =>
            crm.updateCommand(Completed(runId))
            Behaviors.same
        }
      case PrismCommands.IsValid(runId, command, replyTo) if command.commandName == ADCCommand.RetractSelect =>
        replyTo ! Accepted(runId)
        Behaviors.same
    }
  }

  private def inAndMoving(self: ActorRef[PrismCommands]): Behavior[PrismCommands] = {
    logger.info("Prism is now following target")
    val targetModifier = scheduleJobForPrismMovement(self)
    receiveWithDefaultBehavior("moving") {
      case PrismCommands.IsValid(runId, command, replyTo) if command.commandName != ADCCommand.RetractSelect =>
        replyTo ! Accepted(runId)
        Behaviors.same
      case PrismCommands.PrismFollow(targetAngle) =>
        prismAngle.setTarget(targetAngle)
        Behaviors.same
      case PrismCommands.PrismStop(_) =>
        targetModifier.cancel()
        inAndStopped(self)
      case PrismCommands.FollowTarget =>
        publishPrismState(MOVING)
        prismAngle.nextCurrent()
        logger.info(s"Prism current angle ${prismAngle.currentAngle.toDouble}")
        publishEvent(PrismCurrentEvent.make(prismAngle.currentAngle.toDouble, prismAngle.target.toDouble, getCurrentDiff.toDouble))
        Behaviors.same
    }
  }

  private def receiveWithDefaultBehavior(
      state: String
  )(handle: PartialFunction[PrismCommands, Behavior[PrismCommands]]): Behavior[PrismCommands] = {
    Behaviors.receiveMessage(handle.orElse {
      case PrismCommands.IsValid(runId, command, replyTo) =>
        val errMsg = s"Setup command: ${command.commandName.name} is not valid in $state state."
        logger.error(errMsg)
        replyTo ! Invalid(runId, UnsupportedCommandIssue(errMsg))
        Behaviors.same
      case cmd =>
        logger.error(s"$cmd is not valid in $state state.")
        Behaviors.unhandled
    })
  }

  private def startRetracting(runId: Id)(whenDone: => Unit) = {
    timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plus(adcImagerConfiguration.retractSelectDelay))) {
      crm.updateCommand(Completed(runId))
      whenDone
    }
  }

  private def publishEvent(event: SystemEvent)                = eventPublisher.publish(event)
  private def publishRetractPosition(position: PrismPosition) = publishEvent(PrismRetractEvent.make(position))
  private def publishPrismState(state: PrismState) =
    publishEvent(PrismStateEvent.make(state, getCurrentDiff <= tolerance))

  private def scheduleJobForPrismMovement(self: ActorRef[PrismCommands]): Cancellable = {
    timeServiceScheduler.schedulePeriodically(adcImagerConfiguration.targetMovementDelay) {
      if (getCurrentDiff != 0.0) {
        self ! PrismCommands.FollowTarget
      }
    }
  }

  private def getCurrentDiff = prismAngle.target - prismAngle.currentAngle
}

object PrismActor {

  def behavior(cswContext: CswContext, adcImagerConfiguration: AssemblyConfiguration, logger: Logger): Behavior[PrismCommands] =
    new PrismActor(cswContext, adcImagerConfiguration, logger: Logger).setup

}

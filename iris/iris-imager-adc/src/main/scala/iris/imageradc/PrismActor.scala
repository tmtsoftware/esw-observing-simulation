package iris.imageradc

import akka.actor.Cancellable
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.event.api.scaladsl.EventSubscription
import csw.framework.models.CswContext
import csw.params.commands.CommandIssue.UnsupportedCommandIssue
import csw.params.commands.CommandResponse.{Accepted, Completed, Invalid}
import csw.params.core.generics.Parameter
import csw.params.events.SystemEvent
import iris.commons.models.AssemblyConfiguration
import iris.imageradc.commands.{ADCCommand, PrismCommands}
import iris.imageradc.events.TCSElevationAngleEvent.{ElevationEventKey, angleKey}
import iris.imageradc.events.{PrismCurrentEvent, PrismStateEvent, PrismTargetEvent}
import iris.imageradc.models.PrismState.{MOVING, STOPPED}
import iris.imageradc.models.{PrismPosition, PrismState}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class PrismActor(cswContext: CswContext, configuration: AssemblyConfiguration) {
  var prismTarget: Double                        = 0.0
  var prismCurrent: Double                       = 0.0
  var prismState: PrismState                     = PrismState.STOPPED
  var tcsSubscription: Option[EventSubscription] = None
  private val timeServiceScheduler               = cswContext.timeServiceScheduler
  private val crm                                = cswContext.commandResponseManager
  private lazy val eventPublisher                = cswContext.eventService.defaultPublisher
  private lazy val eventSubscriber               = cswContext.eventService.defaultSubscriber

  // assembly is enabled & stopped
  def in: Behavior[PrismCommands] = {
    val subscriptions = publishPrismStatesFor(STOPPED)
    Behaviors.receive { (_, msg) =>
      {
        msg match {
          case PrismCommands.RetractSelect(runId, position) =>
            position match {
              case PrismPosition.IN =>
                crm.updateCommand(Completed(runId))
                Behaviors.same
              case PrismPosition.OUT => out
            }
          case PrismCommands.IsValid(runId, _, replyTo) =>
            replyTo ! Accepted(runId)
            Behaviors.same
          case PrismCommands.PRISM_FOLLOW(_) =>
            subscribeToTCSEvents()
            subscriptions.foreach(_.cancel())
            moving
          case PrismCommands.PRISM_STOP(runId) =>
            crm.updateCommand(Completed(runId))
            Behaviors.same
        }
      }
    }
  }
  // assembly is disabled & stopped
  def out: Behavior[PrismCommands] = {
    Behaviors.receive { (ctx, msg) =>
      {
        val log = cswContext.loggerFactory.getLogger(ctx)
        msg match {
          case PrismCommands.RetractSelect(runId, position) =>
            position match {
              case PrismPosition.IN => in
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
          case PrismCommands.PRISM_FOLLOW(_) =>
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
  // assembly is enabled & moving
  def moving: Behavior[PrismCommands] = {
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
        case PrismCommands.PRISM_FOLLOW(runId) =>
          crm.updateCommand(Completed(runId))
          Behaviors.same
        case PrismCommands.PRISM_STOP(_) =>
          unsubscribeTCSEvents()
          publisherSubscriptions.foreach(_.cancel())
          in
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
    def generatePrismState: Option[SystemEvent] = Option {
      PrismStateEvent.make(state, Math.abs(prismCurrent - prismTarget) < 0.5)
    }
    List(
      eventPublisher.publish(generatePrismCurrent, 1.second),
      eventPublisher.publish(generatePrismTarget, 1.second),
      eventPublisher.publish(generatePrismState, 1.second)
    )
  }
  private def unsubscribeTCSEvents(): Unit = tcsSubscription.foreach(_.unsubscribe())
  private def subscribeToTCSEvents(): Unit =
    tcsSubscription = Some(
      eventSubscriber.subscribeAsync(
        Set(ElevationEventKey),
        e => {
          val targetParameter: Option[Parameter[Double]] = e.paramType.get(angleKey)
          targetParameter match {
            case Some(targetParam) =>
              targetParam.values.headOption.foreach(prismTarget = _)
              scheduleJob
              Future.successful()
            case None => Future.successful()
          }
        }
      )
    )

  private def scheduleJob = ???

  protected val name: String = "Imager ADC"
}

object PrismActor {

  def behavior(cswContext: CswContext, configuration: AssemblyConfiguration): Behavior[PrismCommands] =
    new PrismActor(cswContext, configuration).out

}

package iris.imageradc

import akka.Done
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.framework.models.CswContext
import iris.commons.models.AssemblyConfiguration
import iris.imageradc.commands.{ADCCommand, PrismCommands}
import iris.imageradc.events.{PrismCurrentEvent, PrismStateEvent, PrismTargetEvent}
import iris.imageradc.models.PrismState

import scala.concurrent.Future

class PrismActor(cswContext: CswContext, configuration: AssemblyConfiguration) {
  private lazy val eventPublisher                     = cswContext.eventService.defaultPublisher
  def out(currentAngle: Double): Behavior[PrismCommands] = {
    Behaviors.receiveMessage {
      case PrismCommands.IN => ???
      case PrismCommands.OUT => ???
    }
  }

  def in(current: Double, target: Double): Behavior[PrismCommands] = ???

  protected val name: String = "Imager ADC"
  def publishCurrent(angle: Double, angleError: Double): Future[Done] =
    eventPublisher.publish(PrismCurrentEvent.make(angle, angleError))
  def publishTarget(angle: Double): Future[Done] =
    eventPublisher.publish(PrismTargetEvent.make(angle))
  def publishState(current: PrismState, onTarget: Boolean): Future[Done] =
    eventPublisher.publish(PrismStateEvent.make(current, onTarget))
}

object PrismActor {
  val InitialAngle: Double = 0.0

  def behavior(cswContext: CswContext, configuration: AssemblyConfiguration): Behavior[PrismCommands] =
    new PrismActor(cswContext, configuration).out(InitialAngle)

}

package wfos.redfilter

import akka.Done
import akka.actor.typed.Behavior
import csw.framework.models.CswContext
import wfos.commons.WheelAssembly
import wfos.commons.models.{AssemblyConfiguration, WheelCommand}
import wfos.redfilter.events.RedFilterPositionEvent
import wfos.redfilter.models.FilterWheelPosition

import scala.concurrent.Future

class FilterWheelActor(cswContext: CswContext, configuration: AssemblyConfiguration)
    extends WheelAssembly[FilterWheelPosition](cswContext, configuration) {
  private lazy val eventPublisher = cswContext.eventService.defaultPublisher

  protected val name: String = "Filter Wheel"
  def publishPosition(current: FilterWheelPosition, target: FilterWheelPosition, dark: Boolean): Future[Done] =
    eventPublisher.publish(RedFilterPositionEvent.make(current, target, dark))
}

object FilterWheelActor {
  val InitialPosition: FilterWheelPosition = FilterWheelPosition.Z

  def behavior(cswContext: CswContext, configuration: AssemblyConfiguration): Behavior[WheelCommand[FilterWheelPosition]] =
    new FilterWheelActor(cswContext, configuration).idle(InitialPosition)
}

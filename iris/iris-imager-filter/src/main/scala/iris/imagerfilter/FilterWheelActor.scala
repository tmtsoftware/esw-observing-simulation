package iris.imagerfilter

import org.apache.pekko.Done
import org.apache.pekko.actor.typed.Behavior
import csw.framework.models.CswContext
import iris.commons.WheelAssembly
import iris.commons.models.{WheelCommand, AssemblyConfiguration}
import iris.imagerfilter.events.ImagerPositionEvent
import iris.imagerfilter.models.FilterWheelPosition

import scala.concurrent.Future

class FilterWheelActor(cswContext: CswContext, configuration: AssemblyConfiguration)
    extends WheelAssembly[FilterWheelPosition](cswContext, configuration) {
  private lazy val eventPublisher = cswContext.eventService.defaultPublisher

  protected val name: String = "Filter Wheel"
  def publishPosition(current: FilterWheelPosition, target: FilterWheelPosition, dark: Boolean): Future[Done] =
    eventPublisher.publish(ImagerPositionEvent.make(current, target, dark))
}

object FilterWheelActor {
  val InitialPosition: FilterWheelPosition = FilterWheelPosition.Z

  def behavior(cswContext: CswContext, configuration: AssemblyConfiguration): Behavior[WheelCommand[FilterWheelPosition]] =
    new FilterWheelActor(cswContext, configuration).idle(InitialPosition)
}

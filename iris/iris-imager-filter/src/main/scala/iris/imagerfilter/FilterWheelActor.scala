package iris.imagerfilter

import akka.Done
import akka.actor.typed.Behavior
import csw.framework.models.CswContext
import iris.commons.WheelAssembly
import iris.commons.models.{WheelCommand, WheelConfiguration}
import iris.imagerfilter.events.ImagerPositionEvent
import iris.imagerfilter.models.FilterWheelPosition

import scala.concurrent.Future

class FilterWheelActor(cswContext: CswContext, configuration: WheelConfiguration)
    extends WheelAssembly[FilterWheelPosition](cswContext, configuration) {
  private lazy val eventPublisher = cswContext.eventService.defaultPublisher

  protected val name: String = "Filter Wheel"
  def publishPosition(current: FilterWheelPosition, target: FilterWheelPosition, dark: Boolean): Future[Done] =
    eventPublisher.publish(ImagerPositionEvent.make(current, target, dark))
}

object FilterWheelActor {
  val InitialPosition: FilterWheelPosition = FilterWheelPosition.F1

  def behavior(cswContext: CswContext, configuration: WheelConfiguration): Behavior[WheelCommand[FilterWheelPosition]] =
    new FilterWheelActor(cswContext, configuration).idle(InitialPosition)
}

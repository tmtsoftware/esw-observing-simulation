package iris.imagerfilter

import akka.Done
import akka.actor.typed.{ActorSystem, Behavior}
import csw.framework.models.CswContext
import iris.commons.{WheelAssembly, WheelAssembly2, WheelAssembly3}
import iris.commons.models.{AssemblyConfiguration, WheelCommand}
import iris.imagerfilter.events.ImagerPositionEvent
import iris.imagerfilter.models.FilterWheelPosition

import scala.concurrent.Future

class FilterWheelActor(cswContext: CswContext, configuration: AssemblyConfiguration)
    extends WheelAssembly3[FilterWheelPosition](cswContext, configuration) {
  private lazy val eventPublisher = cswContext.eventService.defaultPublisher

  protected val name: String = "Filter Wheel"
  def publishPosition(current: FilterWheelPosition, target: FilterWheelPosition, dark: Boolean): Future[Done] =
    eventPublisher.publish(ImagerPositionEvent.make(current, target, dark))
}

object FilterWheelActor {
  val InitialPosition: FilterWheelPosition = FilterWheelPosition.Z

  def behavior(cswContext: CswContext, configuration: AssemblyConfiguration)(implicit
      actorSystem: ActorSystem[_]
  ): FilterWheelActor#WheelAssemblyStrand = {
    val filterWheelActor = new FilterWheelActor(cswContext, configuration)
    new filterWheelActor.WheelAssemblyStrand(InitialPosition)
  }
}

package iris.ifsscale

import akka.Done
import akka.actor.typed.Behavior
import csw.framework.models.CswContext
import iris.commons.WheelAssembly
import iris.commons.models.{WheelCommand, AssemblyConfiguration}
import iris.ifsscale.events.IfsScaleEvent
import iris.ifsscale.models.ScaleLevel

import scala.concurrent.Future

class ScaleWheelActor(cswContext: CswContext, configuration: AssemblyConfiguration)
    extends WheelAssembly[ScaleLevel](cswContext, configuration) {
  private lazy val eventPublisher = cswContext.eventService.defaultPublisher

  protected val name: String = "Scale Wheel"
  override def publishPosition(current: ScaleLevel, target: ScaleLevel, dark: Boolean): Future[Done] =
    eventPublisher.publish(IfsScaleEvent.make(current, target))
}

object ScaleWheelActor {
  val InitialScale: ScaleLevel = ScaleLevel.S25

  def behavior(cswContext: CswContext, configuration: AssemblyConfiguration): Behavior[WheelCommand[ScaleLevel]] =
    new ScaleWheelActor(cswContext, configuration).idle(InitialScale)
}

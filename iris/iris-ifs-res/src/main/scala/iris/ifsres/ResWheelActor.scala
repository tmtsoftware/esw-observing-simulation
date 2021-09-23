package iris.ifsres

import akka.Done
import akka.actor.typed.Behavior
import csw.framework.models.CswContext
import iris.commons.WheelAssembly
import iris.commons.models.{WheelCommand, WheelConfiguration}
import iris.ifsres.events.IfsPositionEvent
import iris.ifsres.models.ResWheelPosition

import scala.concurrent.Future

class ResWheelActor(cswContext: CswContext, configuration: WheelConfiguration) extends WheelAssembly[ResWheelPosition](cswContext, configuration){
  private lazy val eventPublisher  = cswContext.eventService.defaultPublisher

  protected val name:String = "Res Wheel"
  override def publishPosition(current: ResWheelPosition, target: ResWheelPosition, dark: Boolean): Future[Done] =
    eventPublisher.publish(IfsPositionEvent.make(current, target))
}

object ResWheelActor {
  val InitialPosition: ResWheelPosition = ResWheelPosition.R4000_Z

  def behavior(cswContext: CswContext, configuration: WheelConfiguration): Behavior[WheelCommand[ResWheelPosition]] =
    new ResWheelActor(cswContext, configuration).idle(InitialPosition)
}

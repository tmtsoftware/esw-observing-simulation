package iris.imager.detector

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}

class ImagerDetectorBehaviorFactory extends ComponentBehaviorFactory {
  override protected def handlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new ImagerDetectorHandlers(ctx, cswCtx)
}

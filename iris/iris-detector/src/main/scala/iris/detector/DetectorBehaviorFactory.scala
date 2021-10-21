package iris.detector

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}

class DetectorBehaviorFactory extends ComponentBehaviorFactory {
  override protected def handlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new DetectorHandlers(ctx, cswCtx)
}

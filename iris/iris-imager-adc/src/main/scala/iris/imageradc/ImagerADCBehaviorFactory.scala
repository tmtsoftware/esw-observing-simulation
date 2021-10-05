package iris.imageradc

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}

class ImagerADCBehaviorFactory extends ComponentBehaviorFactory {
  override def handlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new ImagerADCHandlers(ctx, cswCtx)
}

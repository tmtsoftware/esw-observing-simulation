package iris.ifsres.events

import csw.params.core.generics.GChoiceKey
import csw.params.events.{EventKey, EventName, SystemEvent}
import iris.ifsres.Constants.IfsResAssemblyPrefix
import iris.ifsres.models.ResWheelPosition

object IfsPositionEvent {
  val CurrentPositionKey: GChoiceKey     = ResWheelPosition.makeChoiceKey("current")
  val TargetPositionKey: GChoiceKey      = ResWheelPosition.makeChoiceKey("target")
  val IfsResPositionEventName: EventName = EventName("SpectralResolutionPosition")
  val IfsResPositionEventKey: EventKey   = EventKey(IfsResAssemblyPrefix, IfsResPositionEventName)

  def make(current: ResWheelPosition, target: ResWheelPosition): SystemEvent =
    SystemEvent(
      IfsResAssemblyPrefix,
      IfsResPositionEventName,
      Set(
        CurrentPositionKey.set(current.entryName),
        TargetPositionKey.set(target.entryName)
      )
    )
}

package iris.ifsscale.events

import csw.params.core.generics.GChoiceKey
import csw.params.events.{EventKey, EventName, SystemEvent}
import iris.ifsscale.Constants.IfsScaleAssemblyPrefix
import iris.ifsscale.models.ScaleLevel

object IfsScaleEvent {
  val CurrentScaleKey: GChoiceKey  = ScaleLevel.makeChoiceKey("current")
  val TargetScaleKey: GChoiceKey   = ScaleLevel.makeChoiceKey("target")
  val IfsScaleEventName: EventName = EventName("TargetScale")
  val IfsScaleEventKey: EventKey   = EventKey(IfsScaleAssemblyPrefix, IfsScaleEventName)

  def make(current: ScaleLevel, target: ScaleLevel): SystemEvent =
    SystemEvent(
      IfsScaleAssemblyPrefix,
      IfsScaleEventName,
      Set(
        CurrentScaleKey.set(current.entryName),
        TargetScaleKey.set(target.entryName)
      )
    )
}

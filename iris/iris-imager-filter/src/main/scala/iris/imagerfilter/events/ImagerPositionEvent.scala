package iris.imagerfilter.events

import csw.params.core.generics.KeyType.BooleanKey
import csw.params.core.generics.{GChoiceKey, Key}
import csw.params.events.{EventKey, EventName, SystemEvent}
import iris.imagerfilter.Constants.ImagerFilterAssemblyPrefix
import iris.imagerfilter.models.FilterWheelPosition

object ImagerPositionEvent {
  val CurrentPositionKey: GChoiceKey     = FilterWheelPosition.makeChoiceKey("wheel1CurrentPosition")
  val TargetPositionKey: GChoiceKey      = FilterWheelPosition.makeChoiceKey("wheel1TargetPosition")
  val DarkKey: Key[Boolean]              = BooleanKey.make("darkInserted")
  val ImagerPositionEventName: EventName = EventName("Wheel1Position")
  val ImagerPositionEventKey: EventKey   = EventKey(ImagerFilterAssemblyPrefix, ImagerPositionEventName)

  def make(current: FilterWheelPosition, target: FilterWheelPosition, dark: Boolean): SystemEvent =
    SystemEvent(
      ImagerFilterAssemblyPrefix,
      ImagerPositionEventName,
      Set(
        CurrentPositionKey.set(current.entryName),
        TargetPositionKey.set(target.entryName),
        DarkKey.set(dark)
      )
    )
}

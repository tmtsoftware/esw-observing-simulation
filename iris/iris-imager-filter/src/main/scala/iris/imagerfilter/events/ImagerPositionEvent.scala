package iris.imagerfilter.events

import csw.params.core.generics.KeyType.BooleanKey
import csw.params.core.generics.{GChoiceKey, Key}
import csw.params.events.{EventKey, EventName, SystemEvent}
import iris.imagerfilter.Constants.ImagerFilterAssemblyPrefix
import iris.imagerfilter.models.FilterWheelPosition

object ImagerPositionEvent {
  val CurrentPositionKey: GChoiceKey     = FilterWheelPosition.makeChoiceKey("current")
  val DemandPositionKey: GChoiceKey      = FilterWheelPosition.makeChoiceKey("demand")
  val DarkKey: Key[Boolean]              = BooleanKey.make("dark")
  val ImagerPositionEventName: EventName = EventName("Wheel1Position")
  val ImagerPositionEventKey: EventKey   = EventKey(ImagerFilterAssemblyPrefix, ImagerPositionEventName)

  def make(current: FilterWheelPosition, target: FilterWheelPosition, dark: Boolean): SystemEvent =
    SystemEvent(
      ImagerFilterAssemblyPrefix,
      ImagerPositionEventName,
      Set(
        CurrentPositionKey.set(current.entryName),
        DemandPositionKey.set(target.entryName),
        DarkKey.set(dark)
      )
    )
}

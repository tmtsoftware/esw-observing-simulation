package wfos.filter.events

import csw.params.core.generics.KeyType.BooleanKey
import csw.params.core.generics.{GChoiceKey, Key}
import csw.params.events.{EventKey, EventName, SystemEvent}
import csw.prefix.models.Prefix
import wfos.filter.models.FilterWheelPosition

class FilterPositionEvent(filterPrefix: Prefix) {
  val CurrentPositionKey: GChoiceKey     = FilterWheelPosition.makeChoiceKey("current")
  val DemandPositionKey: GChoiceKey      = FilterWheelPosition.makeChoiceKey("demand")
  val DarkKey: Key[Boolean]              = BooleanKey.make("dark")
  val FilterPositionEventName: EventName = EventName("Wheel1Position")
  val FilterPositionEventKey: EventKey   = EventKey(filterPrefix, FilterPositionEventName)

  def make(current: FilterWheelPosition, target: FilterWheelPosition, dark: Boolean): SystemEvent =
    SystemEvent(
      filterPrefix,
      FilterPositionEventName,
      Set(
        CurrentPositionKey.set(current.entryName),
        DemandPositionKey.set(target.entryName),
        DarkKey.set(dark)
      )
    )
}

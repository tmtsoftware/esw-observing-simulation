package wfos.filter.events

import csw.params.core.generics.KeyType.BooleanKey
import csw.params.core.generics.{GChoiceKey, Key}
import csw.params.events.{EventKey, EventName, SystemEvent}
import csw.prefix.models.Prefix
import wfos.filter.models.{BlueFilterWheelPosition, FilterWheelPosition, RedFilterWheelPosition}

abstract class FilterPositionEvent(filterPrefix: Prefix) {
  val CurrentPositionKey: GChoiceKey
  val DemandPositionKey: GChoiceKey
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

class RedFilterPositionEvent(filterPrefix: Prefix) extends FilterPositionEvent(filterPrefix) {
  override val CurrentPositionKey: GChoiceKey = RedFilterWheelPosition.makeChoiceKey("current")
  override val DemandPositionKey: GChoiceKey  = RedFilterWheelPosition.makeChoiceKey("demand")
}

class BlueFilterPositionEvent(filterPrefix: Prefix) extends FilterPositionEvent(filterPrefix) {
  override val CurrentPositionKey: GChoiceKey = BlueFilterWheelPosition.makeChoiceKey("current")
  override val DemandPositionKey: GChoiceKey  = BlueFilterWheelPosition.makeChoiceKey("demand")
}

package wfos.redfilter.events

import csw.params.core.generics.KeyType.BooleanKey
import csw.params.core.generics.{GChoiceKey, Key}
import csw.params.events.{EventKey, EventName, SystemEvent}
import wfos.redfilter.Constants.RedFilterAssemblyPrefix
import wfos.redfilter.models.FilterWheelPosition

object RedFilterPositionEvent {
  val CurrentPositionKey: GChoiceKey        = FilterWheelPosition.makeChoiceKey("current")
  val DemandPositionKey: GChoiceKey         = FilterWheelPosition.makeChoiceKey("demand")
  val DarkKey: Key[Boolean]                 = BooleanKey.make("dark")
  val RedFilterPositionEventName: EventName = EventName("Wheel1Position")
  val RedFilterPositionEventKey: EventKey   = EventKey(RedFilterAssemblyPrefix, RedFilterPositionEventName)

  def make(current: FilterWheelPosition, target: FilterWheelPosition, dark: Boolean): SystemEvent =
    SystemEvent(
      RedFilterAssemblyPrefix,
      RedFilterPositionEventName,
      Set(
        CurrentPositionKey.set(current.entryName),
        DemandPositionKey.set(target.entryName),
        DarkKey.set(dark)
      )
    )
}

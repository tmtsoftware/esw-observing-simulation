package iris.imageradc.events

import csw.params.core.generics.Key
import csw.params.core.generics.KeyType.DoubleKey
import csw.params.events.{EventKey, EventName, SystemEvent}
import iris.imageradc.Constants.TCSPrefix

object TCSElevationAngleEvent {
  val angleKey: Key[Double]         = DoubleKey.make("targetElevation")
  val ElevationEventName: EventName = EventName("elevation")
  val ElevationEventKey: EventKey   = EventKey(TCSPrefix, ElevationEventName)

  def make(angle: Double): SystemEvent =
    SystemEvent(
      TCSPrefix,
      ElevationEventName,
      Set(
        angleKey.set(angle)
      )
    )
}

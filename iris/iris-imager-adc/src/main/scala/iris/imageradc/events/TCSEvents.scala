package iris.imageradc.events

import csw.params.core.generics.{Key, KeyType}
import csw.params.core.models.Coords.AltAzCoord
import csw.params.events.{EventKey, EventName, SystemEvent}
import iris.imageradc.Constants.TCSPointingKernelPrefix

object TCSEvents {
  val posKey: Key[AltAzCoord]         = KeyType.AltAzCoordKey.make("pos")
  val MountDemandEventName: EventName = EventName("MountDemandPosition")
  val MountDemandKey: EventKey             = EventKey(TCSPointingKernelPrefix, MountDemandEventName)

  def make(angle: AltAzCoord): SystemEvent =
    SystemEvent(
      TCSPointingKernelPrefix,
      MountDemandEventName,
      Set(
        posKey.set(angle)
      )
    )
}

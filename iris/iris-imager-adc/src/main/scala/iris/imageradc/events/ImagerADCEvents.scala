package iris.imageradc.events

import csw.params.core.generics.{GChoiceKey, Key}
import csw.params.core.generics.KeyType.{BooleanKey, DoubleKey}
import csw.params.events.{EventKey, EventName, SystemEvent}
import iris.imageradc.Constants.ImagerADCAssemblyPrefix
import iris.imageradc.models.{PrismPosition, PrismState}

object PrismStateEvent {
  val moveKey: GChoiceKey                = PrismState.makeChoiceKey("move")
  val onTargetKey: Key[Boolean]          = BooleanKey.make("onTarget")
  val ImagerADCStateEventName: EventName = EventName("prism_state")
  val ImagerADCStateEventKey: EventKey   = EventKey(ImagerADCAssemblyPrefix, ImagerADCStateEventName)

  def make(current: PrismState, onTarget: Boolean): SystemEvent =
    SystemEvent(
      ImagerADCAssemblyPrefix,
      ImagerADCStateEventName,
      Set(
        moveKey.set(current.entryName),
        onTargetKey.set(onTarget)
      )
    )
}

object PrismTargetEvent {
  val angleKey: Key[Double]               = DoubleKey.make("angle")
  val ImagerADCTargetEventName: EventName = EventName("prism_target")
  val ImagerADCTargetEventKey: EventKey   = EventKey(ImagerADCAssemblyPrefix, ImagerADCTargetEventName)

  def make(angle: Double): SystemEvent =
    SystemEvent(
      ImagerADCAssemblyPrefix,
      ImagerADCTargetEventName,
      Set(
        angleKey.set(angle)
      )
    )
}

object PrismCurrentEvent {
  val angleKey: Key[Double]                = DoubleKey.make("angle")
  val angleErrorKey: Key[Double]           = DoubleKey.make("angle_error")
  val ImagerADCCurrentEventName: EventName = EventName("prism_current")
  val ImagerADCCurrentEventKey: EventKey   = EventKey(ImagerADCAssemblyPrefix, ImagerADCCurrentEventName)

  def make(angle: Double, angleError: Double): SystemEvent =
    SystemEvent(
      ImagerADCAssemblyPrefix,
      ImagerADCCurrentEventName,
      Set(
        angleKey.set(angle),
        angleErrorKey.set(angleError)
      )
    )
}

object PrismRetractEvent {
  val ImagerADCRetractEventName: EventName = EventName("prism_position")
  val ImagerADCRetractEventKey: EventKey   = EventKey(ImagerADCAssemblyPrefix, ImagerADCRetractEventName)

  def make(state: PrismPosition): SystemEvent =
    SystemEvent(
      ImagerADCAssemblyPrefix,
      ImagerADCRetractEventName,
      Set(
        PrismPosition.RetractKey.set(state.entryName)
      )
    )
}

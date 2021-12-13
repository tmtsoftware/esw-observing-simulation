package iris.imageradc.events

import csw.params.core.generics.KeyType.{BooleanKey, DoubleKey}
import csw.params.core.generics.{GChoiceKey, Key}
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

object PrismCurrentEvent {
  val currentAngleKey: Key[Double]         = DoubleKey.make("currentAngle")
  val angleErrorKey: Key[Double]           = DoubleKey.make("errorAngle")
  val targetAngleKey: Key[Double]          = DoubleKey.make("targetAngle")
  val ImagerADCCurrentEventName: EventName = EventName("prism_current")
  val ImagerADCCurrentEventKey: EventKey   = EventKey(ImagerADCAssemblyPrefix, ImagerADCCurrentEventName)

  def make(currentAngle: Double, targetAngle: Double, errorAngle: Double): SystemEvent =
    SystemEvent(
      ImagerADCAssemblyPrefix,
      ImagerADCCurrentEventName,
      Set(
        currentAngleKey.set(currentAngle),
        angleErrorKey.set(errorAngle),
        targetAngleKey.set(targetAngle)
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

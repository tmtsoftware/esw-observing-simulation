package esw.observing.simulation

import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.params.commands.{CommandName, Observe, Sequence, Setup}
import csw.params.core.generics.KeyType.{BooleanKey, ChoiceKey, IntKey, StringKey}
import csw.params.core.generics.{GChoiceKey, Key, Parameter}
import csw.params.core.models.{Choices, ObsId, Units}
import csw.params.events.{EventKey, EventName, ObserveEventNames}
import csw.prefix.models.Subsystem.Container
import csw.prefix.models.{Prefix, Subsystem}

object WFOSTestData {
  val filterPositionEventName: EventName = EventName("Wheel1Position")
  val filterDarkKey: Key[Boolean]        = BooleanKey.make("dark")

  val redFilterChoices: Choices               = Choices.from("r'", "i'", "z'", "fused-silica")
  val redFilterCurrentPositionKey: GChoiceKey = ChoiceKey.make("current", redFilterChoices)
  val redFilterDemandPositionKey: GChoiceKey  = ChoiceKey.make("demand", redFilterChoices)
  val redFilterKey: GChoiceKey                = ChoiceKey.make("redFilter", Units.NoUnits, redFilterChoices)

  val blueFilterChoices: Choices               = Choices.from("u'", "g'", "fused-silica")
  val blueFilterCurrentPositionKey: GChoiceKey = ChoiceKey.make("current", blueFilterChoices)
  val blueFilterDemandPositionKey: GChoiceKey  = ChoiceKey.make("demand", blueFilterChoices)
  val blueFilterKey: GChoiceKey                = ChoiceKey.make("blueFilter", Units.NoUnits, blueFilterChoices)

  val wfosContainerConnection: AkkaConnection = AkkaConnection(
    ComponentId(Prefix(Container, "WfosContainer"), ComponentType.Container)
  )

  val wfosRedFilterPrefix: Prefix        = Prefix(Subsystem.WFOS, "red.filter")
  val wfosBlueFilterPrefix: Prefix       = Prefix(Subsystem.WFOS, "blue.filter")
  val wfosRedPositionEventKey: EventKey  = EventKey(wfosRedFilterPrefix, filterPositionEventName)
  val wfosBluePositionEventKey: EventKey = EventKey(wfosBlueFilterPrefix, filterPositionEventName)
  val wfosBlueDetectorPrefix: Prefix     = Prefix("WFOS.blue.detector")
  val wfosRedDetectorPrefix: Prefix      = Prefix("WFOS.red.detector")

  val obsId: Option[ObsId]          = Some(ObsId("2020A-001-123"))
  val directoryP: Parameter[String] = StringKey.make("directory").set("/tmp")

  val blueExposureIdP: Parameter[String]   = StringKey.make("blueExposureId").set("2020A-001-123-IRIS-IMG-DRK1-0023")
  val blueIntegrationTimeP: Parameter[Int] = IntKey.make("blueIntegrationTime").set(2000)
  val blueNumRampsP: Parameter[Int]        = IntKey.make("blueNumRamps").set(2)

  val redExposureIdP: Parameter[String]   = StringKey.make("redExposureId").set("2020A-001-123-IRIS-IMG-DRK1-0023")
  val redIntegrationTimeP: Parameter[Int] = IntKey.make("redIntegrationTime").set(2000)
  val redNumRampsP: Parameter[Int]        = IntKey.make("redNumRamps").set(2)

  private val wfosSourcePrefix: Prefix = Prefix("ESW.wfos_science")

  val setupAcquisition: Setup = Setup(wfosSourcePrefix, CommandName("setupAcquisition"), obsId).add(
    blueFilterKey.set("g'")
  )

  val setupObservation: Setup = Setup(wfosSourcePrefix, CommandName("setupObservation"), obsId).madd(
    blueFilterKey.set("fused-silica"),
    redFilterKey.set("z'")
  )

  val acquisitionExposure: Observe = Observe(wfosSourcePrefix, CommandName("acquisitionExposure"), obsId).madd(
    directoryP,
    blueExposureIdP,
    blueIntegrationTimeP,
    blueNumRampsP
  )

  val singleExposure: Observe = Observe(wfosSourcePrefix, CommandName("singleExposure"), obsId).madd(
    directoryP,
    blueExposureIdP,
    redExposureIdP,
    blueIntegrationTimeP,
    redIntegrationTimeP,
    blueNumRampsP,
    redNumRampsP
  )

  def detectorObsEvents(detectorPrefix: Prefix) = Set(
    EventKey(detectorPrefix, ObserveEventNames.ExposureStart),
    EventKey(detectorPrefix, ObserveEventNames.ExposureEnd),
    EventKey(detectorPrefix, ObserveEventNames.ExposureAborted),
    EventKey(detectorPrefix, ObserveEventNames.DataWriteEnd),
    EventKey(detectorPrefix, ObserveEventNames.DataWriteStart)
  )

  val sequence: Sequence = Sequence(setupAcquisition, acquisitionExposure, setupObservation, singleExposure)

}

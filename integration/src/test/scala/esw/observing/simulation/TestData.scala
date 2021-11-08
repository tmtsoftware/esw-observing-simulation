package esw.observing.simulation

import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.params.commands.{CommandName, Observe, Sequence, Setup}
import csw.params.core.generics.KeyType._
import csw.params.core.generics.{GChoiceKey, Key, Parameter}
import csw.params.core.models.{Choice, Choices, ObsId, Units}
import csw.params.events.{EventKey, EventName, ObserveEventNames}
import csw.prefix.models.Subsystem.{Container, IRIS}
import csw.prefix.models.{Prefix, Subsystem}

object TestData {

  //imager filter
  private val filterChoices: Choices             = Choices.from("H")
  val ImagerFilterCurrentPositionKey: GChoiceKey = ChoiceKey.make("current", filterChoices)
  val ImagerFilterDemandPositionKey: GChoiceKey  = ChoiceKey.make("demand", filterChoices)
  val ImagerFilterDarkKey: Key[Boolean]          = BooleanKey.make("dark")

  val filterP: Parameter[Choice] = ChoiceKey.make("filter", Units.NoUnits, filterChoices).set("H")

  //ifs res
  private val ifsResChoices: Choices       = Choices.from("4000-H")
  val IfsResCurrentPositionKey: GChoiceKey = ChoiceKey.make("current", ifsResChoices)
  val IfsResTargetPositionKey: GChoiceKey  = ChoiceKey.make("target", ifsResChoices)

  val spectralResolutionP: Parameter[Choice] =
    ChoiceKey.make("spectralResolution", Units.NoUnits, ifsResChoices).set("4000-H")

  //ifs scale
  private val ifsScaleChoices: Choices       = Choices.from("9")
  val IfsScaleCurrentPositionKey: GChoiceKey = ChoiceKey.make("current", ifsScaleChoices)
  val IfsScaleTargetPositionKey: GChoiceKey  = ChoiceKey.make("target", ifsScaleChoices)

  val IfsScaleP: Parameter[Choice] = ChoiceKey.make("scale", Units.marcsec, ifsScaleChoices).set("9")

  // adc
  val adcPrismRetractKey: GChoiceKey     = ChoiceKey.make("position", Choices.from("IN", "OUT"))
  val adcPrismStateKey: GChoiceKey       = ChoiceKey.make("move", Choices.from("MOVING", "STOPPED"))
  val adcPrismAngleKey: Key[Double]      = DoubleKey.make("angle")
  val adcPrismAngleErrorKey: Key[Double] = DoubleKey.make("angle_error")
  val adcPrismOnTargetKey: Key[Boolean]  = BooleanKey.make("onTarget")

  val scienceAdcFollowP: Parameter[Boolean] = BooleanKey.make("scienceAdcFollow").set(true)
  val scienceAdcTargetP: Parameter[Double]  = DoubleKey.make("scienceAdcTarget").set(40)

  //***********
  val directoryP: Parameter[String]          = StringKey.make("directory").set("/tmp")
  val imagerExposureIdP: Parameter[String]   = StringKey.make("imagerExposureId").set("2020A-001-123-IRIS-IMG-DRK1-0023")
  val ifsExposureIdP: Parameter[String]      = StringKey.make("ifsExposureId").set("2020A-001-123-IRIS-IMG-DRK1-0023")
  val imagerIntegrationTimeP: Parameter[Int] = IntKey.make("imagerIntegrationTime").set(100)
  val ifsIntegrationTimeP: Parameter[Int]    = IntKey.make("ifsIntegrationTime").set(100)
  val imagerNumRampsP: Parameter[Int]        = IntKey.make("imagerNumRamps").set(10)
  val ifsNumRampsP: Parameter[Int]           = IntKey.make("ifsNumRamps").set(10)
  val obsId: Option[ObsId]                   = Some(ObsId("2020A-001-123"))

  val setup: Setup = Setup(Prefix("IRIS.Imager"), CommandName("setupObservation"), obsId).madd(
    filterP,
    IfsScaleP,
    spectralResolutionP,
    scienceAdcFollowP,
    scienceAdcTargetP
  )

  val observe: Observe = Observe(Prefix("IRIS.Imager"), CommandName("singleExposure"), obsId).madd(
    directoryP,
    imagerExposureIdP,
    ifsExposureIdP,
    imagerIntegrationTimeP,
    ifsIntegrationTimeP,
    imagerNumRampsP,
    ifsNumRampsP
  )

  val imagerFilterPrefix: Prefix               = Prefix(Subsystem.IRIS, "imager.filter")
  val imagerFilterPositionEventName: EventName = EventName("Wheel1Position")
  val imagerFilterPositionEventKey: EventKey   = EventKey(imagerFilterPrefix, imagerFilterPositionEventName)

  val IfsResAssemblyPrefix: Prefix       = Prefix(IRIS, "ifs.res")
  val IfsResPositionEventName: EventName = EventName("SpectralResolutionPosition")
  val IfsResPositionEventKey: EventKey   = EventKey(IfsResAssemblyPrefix, IfsResPositionEventName)

  val IfsScaleAssemblyPrefix: Prefix = Prefix(IRIS, "ifs.scale")
  val IfsScaleEventName: EventName   = EventName("TargetScale")
  val IfsScaleEventKey: EventKey     = EventKey(IfsScaleAssemblyPrefix, IfsScaleEventName)

  val ImagerADCAssemblyPrefix: Prefix      = Prefix(IRIS, "imager.adc")
  val ImagerADCStateEventName: EventName   = EventName("prism_state")
  val ImagerADCStateEventKey: EventKey     = EventKey(ImagerADCAssemblyPrefix, ImagerADCStateEventName)
  val ImagerADCTargetEventName: EventName  = EventName("prism_target")
  val ImagerADCTargetEventKey: EventKey    = EventKey(ImagerADCAssemblyPrefix, ImagerADCTargetEventName)
  val ImagerADCRetractEventName: EventName = EventName("prism_position")
  val ImagerADCRetractEventKey: EventKey   = EventKey(ImagerADCAssemblyPrefix, ImagerADCRetractEventName)
  val ImagerADCCurrentEventName: EventName = EventName("prism_current")
  val ImagerADCCurrentEventKey: EventKey   = EventKey(ImagerADCAssemblyPrefix, ImagerADCCurrentEventName)

  val ImagerDetectorPrefix: Prefix = Prefix("IRIS.imager.detector")
  val IfsDetectorPrefix: Prefix    = Prefix("IRIS.ifs.detector")

  val irisContainerConnection: AkkaConnection = AkkaConnection(
    ComponentId(Prefix(Container, "IrisContainer"), ComponentType.Container)
  )

  def detectorObsEvents(detectorPrefix: Prefix) = Set(
    EventKey(detectorPrefix, ObserveEventNames.ExposureStart),
    EventKey(detectorPrefix, ObserveEventNames.ExposureEnd),
    EventKey(detectorPrefix, ObserveEventNames.ExposureAborted),
    EventKey(detectorPrefix, ObserveEventNames.DataWriteEnd),
    EventKey(detectorPrefix, ObserveEventNames.DataWriteStart)
  )

  val sequence: Sequence = Sequence(setup, observe)
}

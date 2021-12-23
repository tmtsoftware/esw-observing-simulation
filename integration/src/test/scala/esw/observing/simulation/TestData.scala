package esw.observing.simulation

import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.params.commands.{CommandName, Observe, Sequence, Setup}
import csw.params.core.generics.KeyType._
import csw.params.core.generics.{GChoiceKey, Key, Parameter}
import csw.params.core.models.Coords.EqCoord
import csw.params.core.models._
import csw.params.events.{EventKey, EventName, ObserveEventNames}
import csw.prefix.models.Subsystem.{Container, IRIS, TCS}
import csw.prefix.models.{Prefix, Subsystem}

object TestData {

  //imager filter
  private val filterChoices: Choices             = Choices.from("Ks", "CO")
  val ImagerFilterCurrentPositionKey: GChoiceKey = ChoiceKey.make("current", filterChoices)
  val ImagerFilterDemandPositionKey: GChoiceKey  = ChoiceKey.make("demand", filterChoices)
  val ImagerFilterDarkKey: Key[Boolean]          = BooleanKey.make("dark")

  val filterKey: GChoiceKey = ChoiceKey.make("filter", Units.NoUnits, filterChoices)

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
  val adcPrismRetractKey: GChoiceKey      = ChoiceKey.make("position", Choices.from("IN", "OUT"))
  val adcPrismStateKey: GChoiceKey        = ChoiceKey.make("move", Choices.from("MOVING", "STOPPED"))
  val adcPrismAngleKey: Key[Double]       = DoubleKey.make("currentAngle")
  val adcPrismTargetAngleKey: Key[Double] = DoubleKey.make("targetAngle")
  val adcPrismAngleErrorKey: Key[Double]  = DoubleKey.make("errorAngle")
  val adcPrismOnTargetKey: Key[Boolean]   = BooleanKey.make("onTarget")

  val scienceAdcFollowP: Parameter[Boolean] = BooleanKey.make("scienceAdcFollow").set(true)

  //***********
  val directoryP: Parameter[String]          = StringKey.make("directory").set("/tmp")
  val imagerExposureIdP: Parameter[String]   = StringKey.make("imagerExposureId").set("2020A-001-123-IRIS-IMG-DRK1-0023")
  val ifsExposureIdP: Parameter[String]      = StringKey.make("ifsExposureId").set("2020A-001-123-IRIS-IMG-DRK1-0023")
  val imagerIntegrationTimeP: Parameter[Int] = IntKey.make("imagerIntegrationTime").set(2000)
  val ifsIntegrationTimeP: Parameter[Int]    = IntKey.make("ifsIntegrationTime").set(2000)
  val imagerNumRampsP: Parameter[Int]        = IntKey.make("imagerNumRamps").set(2)
  val ifsNumRampsP: Parameter[Int]           = IntKey.make("ifsNumRamps").set(2)

  //TCS Sequencer data

  val obsId: Option[ObsId]            = Some(ObsId("2020A-001-123"))
  val baseCoords: Key[Coords.Coord]   = CoordKey.make("baseCoords")
  val targetCoords: Key[Coords.Coord] = CoordKey.make("targetCoords")
  val tcsSequencerPrefix: Prefix      = Prefix("TCS.IRIS_ImagerAndIFS")
  val tcsPreset: Setup = Setup(tcsSequencerPrefix, CommandName("preset"), obsId).madd(
    baseCoords.set(EqCoord(240.0, 120.0))
  )

  private val pKey: Key[Double] = DoubleKey.make("p")
  private val qKey: Key[Double] = DoubleKey.make("q")
  val tcsSetupObservation: Setup = Setup(tcsSequencerPrefix, CommandName("setupObservation"), obsId).madd(
    pKey.set(200.0),
    qKey.set(100.0)
  )

  //IRIS Sequencer data

  val observationStart: Setup = Setup(Prefix("IRIS.Imager"), CommandName("observationStart"), obsId)
  val observationEnd: Setup   = Setup(Prefix("IRIS.Imager"), CommandName("observationEnd"), obsId)

  val setupAcquisition: Setup = Setup(Prefix("IRIS.Imager"), CommandName("setupAcquisition"), obsId).madd(
    filterKey.set("Ks"),
    scienceAdcFollowP
  )

  val setupObservation: Setup = Setup(Prefix("IRIS.Imager"), CommandName("setupObservation"), obsId).madd(
    filterKey.set("CO"),
    IfsScaleP,
    spectralResolutionP,
    scienceAdcFollowP,
    pKey.set(200.0),
    qKey.set(100.0)
  )

  val acquisitionExposure: Observe = Observe(Prefix("IRIS.Imager"), CommandName("acquisitionExposure"), obsId).madd(
    directoryP,
    imagerExposureIdP,
    imagerIntegrationTimeP,
    imagerNumRampsP
  )

  val singleExposure: Observe = Observe(Prefix("IRIS.Imager"), CommandName("singleExposure"), obsId).madd(
    directoryP,
    imagerExposureIdP,
    ifsExposureIdP,
    imagerIntegrationTimeP,
    ifsIntegrationTimeP,
    imagerNumRampsP,
    ifsNumRampsP
  )

  val mcsDemandPositionEventKey: EventKey  = EventKey(Prefix("TCS.MCSAssembly"), EventName("MountPosition"))
  val encCurrentPositionEventKey: EventKey = EventKey(Prefix("TCS.ENCAssembly"), EventName("CurrentPosition"))

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
  val ImagerADCRetractEventName: EventName = EventName("prism_position")
  val ImagerADCRetractEventKey: EventKey   = EventKey(ImagerADCAssemblyPrefix, ImagerADCRetractEventName)
  val ImagerADCCurrentEventName: EventName = EventName("prism_current")
  val ImagerADCCurrentEventKey: EventKey   = EventKey(ImagerADCAssemblyPrefix, ImagerADCCurrentEventName)

  //ESW sequencer data
  private val eswSequencerPrefix: Prefix = Prefix("ESW.IRIS_ImagerAndIFS")

  val eswObservationStart: Setup = Setup(eswSequencerPrefix, CommandName("observationStart"), obsId)
  val eswObservationEnd: Setup   = Setup(eswSequencerPrefix, CommandName("observationEnd"), obsId)

  val preset: Setup = Setup(eswSequencerPrefix, CommandName("preset"), obsId).madd(
    filterKey.set("Ks"),
    scienceAdcFollowP,
    targetCoords.set(EqCoord(240.0, 120.0))
  )

  private val imagerExposureTypeKey = StringKey.make("imagerExposureType")
  private val ifsExposureTypeKey    = StringKey.make("ifsExposureType")

  val coarseAcquisition: Observe = Observe(eswSequencerPrefix, CommandName("coarseAcquisition"), obsId).madd(
    directoryP,
    imagerExposureIdP,
    imagerIntegrationTimeP,
    imagerNumRampsP,
    imagerExposureTypeKey.set("SKY")
  )

  val fineAcquisition: Observe = Observe(eswSequencerPrefix, CommandName("fineAcquisition"), obsId)

  val observe: Observe = Observe(eswSequencerPrefix, CommandName("observe"), obsId).madd(
    directoryP,
    imagerExposureIdP,
    ifsExposureIdP,
    imagerIntegrationTimeP,
    ifsIntegrationTimeP,
    imagerNumRampsP,
    ifsNumRampsP,
    imagerExposureTypeKey.set("SKY"),
    ifsExposureTypeKey.set("SKY")
  )

  private val observationStartKey: EventKey  = EventKey(eswSequencerPrefix, ObserveEventNames.ObservationStart)
  private val observationEndKey: EventKey    = EventKey(eswSequencerPrefix, ObserveEventNames.ObservationEnd)
  private val presetStartKey: EventKey       = EventKey(eswSequencerPrefix, ObserveEventNames.PresetStart)
  private val presetEndKey: EventKey         = EventKey(eswSequencerPrefix, ObserveEventNames.PresetEnd)
  private val guidestarAcqStartKey: EventKey = EventKey(eswSequencerPrefix, ObserveEventNames.GuidestarAcqStart)
  private val guidestarAcqEndKey: EventKey   = EventKey(eswSequencerPrefix, ObserveEventNames.GuidestarAcqEnd)
  private val scitargetAcqStartKey: EventKey = EventKey(eswSequencerPrefix, ObserveEventNames.ScitargetAcqStart)
  private val scitargetAcqEndKey: EventKey   = EventKey(eswSequencerPrefix, ObserveEventNames.ScitargetAcqEnd)
  private val observeStartKey: EventKey      = EventKey(eswSequencerPrefix, ObserveEventNames.ObserveStart)
  private val observeEndKey: EventKey        = EventKey(eswSequencerPrefix, ObserveEventNames.ObserveEnd)

  val observeEventKeys = Set(
    observationStartKey,
    observationEndKey,
    presetStartKey,
    presetEndKey,
    guidestarAcqStartKey,
    guidestarAcqEndKey,
    scitargetAcqStartKey,
    scitargetAcqEndKey,
    observeStartKey,
    observeEndKey
  )

  val ImagerDetectorPrefix: Prefix = Prefix("IRIS.imager.detector")
  val IfsDetectorPrefix: Prefix    = Prefix("IRIS.ifs.detector")

  val irisContainerConnection: AkkaConnection = AkkaConnection(
    ComponentId(Prefix(Container, "IrisContainer"), ComponentType.Container)
  )

  val tcsContainerConnection: AkkaConnection = AkkaConnection(
    ComponentId(Prefix(Container, "TcsContainer"), ComponentType.Container)
  )

  val tcsPkAssemblyConnection: AkkaConnection = AkkaConnection(
    ComponentId(Prefix(TCS, "PointingKernelAssembly"), ComponentType.Assembly)
  )

  val tcsMcsAssemblyConnection: AkkaConnection = AkkaConnection(
    ComponentId(Prefix(TCS, "MCSAssembly"), ComponentType.Assembly)
  )

  val tcsEncAssemblyConnection: AkkaConnection = AkkaConnection(
    ComponentId(Prefix(TCS, "ENCAssembly"), ComponentType.Assembly)
  )

  def detectorObsEvents(detectorPrefix: Prefix) = Set(
    EventKey(detectorPrefix, ObserveEventNames.ExposureStart),
    EventKey(detectorPrefix, ObserveEventNames.ExposureEnd),
    EventKey(detectorPrefix, ObserveEventNames.ExposureAborted),
    EventKey(detectorPrefix, ObserveEventNames.DataWriteEnd),
    EventKey(detectorPrefix, ObserveEventNames.DataWriteStart)
  )

  val tcsSequence: Sequence = Sequence(
    tcsPreset,
    tcsSetupObservation
  )

  val irisSequence: Sequence = Sequence(
    observationStart,
    setupAcquisition,
    acquisitionExposure,
    setupObservation,
    singleExposure,
    setupObservation,
    singleExposure,
    observationEnd
  )

  val eswSequence: Sequence = Sequence(
    eswObservationStart,
    preset,
    coarseAcquisition,
    fineAcquisition,
    setupObservation,
    observe,
    observationEnd
  )
}

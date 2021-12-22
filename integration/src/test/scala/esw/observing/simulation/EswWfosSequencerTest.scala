package esw.observing.simulation

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.framework.deploy.containercmd.ContainerCmd
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.commands.CommandResponse
import csw.params.core.models.ExposureId
import csw.params.events.{Event, ObserveEvent, ObserveEventKeys, ObserveEventNames}
import csw.prefix.models.Subsystem.WFOS
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.CSWService.EventServer
import esw.agent.akka.client.AgentClient
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.MachineAgent
import esw.sm.api.protocol.StartSequencerResponse.Started
import esw.sm.impl.utils.{SequenceComponentAllocator, SequenceComponentUtil}

import java.io.Closeable
import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

class EswWfosSequencerTest extends EswTestKit(EventServer, MachineAgent) {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(1.minute, 100.millis)

  private val obsMode                         = ObsMode("WFOS_Science")
  private val seqComponentName1               = "testComponent1"
  private val seqComponentName2               = "testComponent2"
  private val agentConnection: AkkaConnection = AkkaConnection(ComponentId(agentSettings.prefix, ComponentType.Machine))
  private val testSeqCompConnection1 = AkkaConnection(
    ComponentId(Prefix(agentSettings.prefix.subsystem, seqComponentName1), ComponentType.SequenceComponent)
  )
  private val testSeqCompConnection2 = AkkaConnection(
    ComponentId(Prefix(agentSettings.prefix.subsystem, seqComponentName2), ComponentType.SequenceComponent)
  )

  private lazy val agentLoc    = locationService.find(agentConnection).futureValue
  private lazy val agentClient = new AgentClient(agentLoc.get)

  private val locationServiceUtil               = new LocationServiceUtil(locationService)
  private val sequenceComponentUtil             = new SequenceComponentUtil(locationServiceUtil, new SequenceComponentAllocator())
  private var seqComp1Loc: Option[AkkaLocation] = None
  private var seqComp2Loc: Option[AkkaLocation] = None
  private var containerCmd: Option[Closeable]   = None

  override def afterAll(): Unit = {
    containerCmd.foreach(_.close())
    seqComp1Loc.map(seqCompLocation => agentClient.killComponent(seqCompLocation).futureValue)
    seqComp2Loc.map(seqCompLocation => agentClient.killComponent(seqCompLocation).futureValue)
    super.afterAll()
  }

  /*
   This test submits the sequence to top level ESW sequencer for an observation, it internally submits sequences corresponding to each
   command to downstream sequencer i.e. WFOS in our case. Now, WFOS sequencer send commands to respective assemblies.
   We subscribe and validate all observe events are published by ESW sequencer and in correct order.
   */
  "Wfos top level esw sequencer" must {
    "handle the submitted sequence | ESW-564, ESW-82" in {

      val containerConfPath = Paths.get(getClass.getResource("/WfosContainer.conf").toURI)

      //spawn the wfos container
      containerCmd = Some(ContainerCmd.start("wfos_container_cmd_app", WFOS, List("--local", containerConfPath.toString).toArray))

      Thread.sleep(10000)
      val containerLocation: Option[AkkaLocation] =
        locationService.resolve(WFOSTestData.wfosContainerConnection, 5.seconds).futureValue
      containerLocation.isDefined shouldBe true

      locationService
        .resolve(AkkaConnection(ComponentId(WFOSTestData.wfosBlueFilterPrefix, ComponentType.Assembly)), 5.seconds)
        .futureValue
        .value

      locationService
        .resolve(AkkaConnection(ComponentId(WFOSTestData.wfosRedFilterPrefix, ComponentType.Assembly)), 5.seconds)
        .futureValue
        .value

      locationService
        .resolve(AkkaConnection(ComponentId(WFOSTestData.wfosRedDetectorPrefix, ComponentType.Assembly)), 5.seconds)
        .futureValue
        .value

      locationService
        .resolve(AkkaConnection(ComponentId(WFOSTestData.wfosBlueDetectorPrefix, ComponentType.Assembly)), 5.seconds)
        .futureValue
        .value

      //********************************************************************

      //spawn esw and wfos sequencer
      agentClient.spawnSequenceComponent(seqComponentName1, Some(ScriptVersion.value)).futureValue
      agentClient.spawnSequenceComponent(seqComponentName2, Some(ScriptVersion.value)).futureValue

      seqComp1Loc = locationService.find(testSeqCompConnection1).futureValue
      seqComp2Loc = locationService.find(testSeqCompConnection2).futureValue

      seqComp1Loc.isDefined shouldBe true
      seqComp2Loc.isDefined shouldBe true

      val eswSequencerResponse = sequenceComponentUtil.loadScript(Subsystem.ESW, obsMode, None, seqComp1Loc.get).futureValue
      eswSequencerResponse.rightValue shouldBe a[Started]

      val wfosSequencerResponse = sequenceComponentUtil.loadScript(Subsystem.WFOS, obsMode, None, seqComp2Loc.get).futureValue
      wfosSequencerResponse.rightValue shouldBe a[Started]

      //********************************************************************

      val dmsConsumerProbe          = createTestProbe(WFOSTestData.observeEventKeys)
      val wfosBlueDetectorTestProbe = createTestProbe(WFOSTestData.detectorObsEvents(WFOSTestData.wfosBlueDetectorPrefix))
      val wfosRedDetectorTestProbe  = createTestProbe(WFOSTestData.detectorObsEvents(WFOSTestData.wfosRedDetectorPrefix))

      val sequencerApi = sequencerClient(Subsystem.ESW, obsMode)

      val initialSubmitRes = sequencerApi.submit(WFOSTestData.eswSequence).futureValue
      initialSubmitRes shouldBe a[CommandResponse.Started]

      assertObserveEvents(dmsConsumerProbe, wfosBlueDetectorTestProbe, wfosRedDetectorTestProbe)
    }
  }

  //ESW-82
  private def assertObserveEvents(
      seqTestProbe: TestProbe[Event],
      blueTestProbe: TestProbe[Event],
      redTesProbe: TestProbe[Event]
  ) = {
    //sequence : eswObservationStart,preset,coarseAcquisition,fineAcquisition,setupObservation,observe,observationEnd

    eventually {
      val event = seqTestProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.ObservationStart.name
    }

    eventually {
      val event = seqTestProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.PresetStart.name
    }

    eventually {
      val event = seqTestProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.PresetEnd.name
    }

    eventually {
      val event = seqTestProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.GuidestarAcqStart.name
    }

    eventually {
      val event = seqTestProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.GuidestarAcqEnd.name
    }

    eventually {
      val event = seqTestProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.ScitargetAcqStart.name
    }

    eventually {
      val event = seqTestProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.ScitargetAcqEnd.name
    }

    eventually {
      val event = seqTestProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.ObserveStart.name
    }

    assertDetectorEvents(blueTestProbe, "/tmp", ExposureId("2020A-001-123-IRIS-BLU-SKY1-0002"))
    assertDetectorEvents(redTesProbe, "/tmp", ExposureId("2020A-001-123-IRIS-RED-SKY1-0002"))

    eventually {
      val event = seqTestProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.ObserveEnd.name
    }

    eventually {
      val event = seqTestProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.ObservationEnd.name
    }
  }

  private def assertDetectorEvents(testProbe: TestProbe[Event], directory: String, exposureId: ExposureId) = {
    val filename = s"$directory/$exposureId.fits"

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.ExposureStart.name
      ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.ExposureEnd.name
      ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.DataWriteStart.name
      event(ObserveEventKeys.filename).head shouldBe filename
      ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.DataWriteEnd.name
      event(ObserveEventKeys.filename).head shouldBe filename
      ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
    }

  }
}

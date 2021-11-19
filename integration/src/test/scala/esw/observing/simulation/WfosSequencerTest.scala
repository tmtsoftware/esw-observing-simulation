package esw.observing.simulation

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.framework.deploy.containercmd.ContainerCmd
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.commands.CommandResponse
import csw.params.core.models.ExposureId
import csw.params.events._
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

class WfosSequencerTest extends EswTestKit(EventServer, MachineAgent) {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(1.minute, 100.millis)

  private val sequencerScriptSha              = "87ac72f"
  private val obsMode                         = ObsMode("WFOS_Science")
  private val seqComponentName                = "testComponent"
  private val agentConnection: AkkaConnection = AkkaConnection(ComponentId(agentSettings.prefix, ComponentType.Machine))
  private val testSeqCompConnection = AkkaConnection(
    ComponentId(Prefix(agentSettings.prefix.subsystem, seqComponentName), ComponentType.SequenceComponent)
  )

  private lazy val agentLoc    = locationService.find(agentConnection).futureValue
  private lazy val agentClient = new AgentClient(agentLoc.get)

  private val locationServiceUtil              = new LocationServiceUtil(locationService)
  private val sequenceComponentUtil            = new SequenceComponentUtil(locationServiceUtil, new SequenceComponentAllocator())
  private var seqCompLoc: Option[AkkaLocation] = None
  private var containerCmd: Option[Closeable]  = None

  override def afterAll(): Unit = {
    containerCmd.foreach(_.close())
    seqCompLoc.map(seqCompLocation => agentClient.killComponent(seqCompLocation).futureValue)
    super.afterAll()
  }

  "wfos sequencer" must {
    "handle the submitted sequence | ESW-551" in {
      val wfosBlueFilterTestProbe = createTestProbe(Set(WFOSTestData.wfosBluePositionEventKey))
      val wfosRedFilterTestProbe  = createTestProbe(Set(WFOSTestData.wfosRedPositionEventKey))

      val wfosBlueDetectorTestProbe = createTestProbe(WFOSTestData.detectorObsEvents(WFOSTestData.wfosBlueDetectorPrefix))
      val wfosRedDetectorTestProbe  = createTestProbe(WFOSTestData.detectorObsEvents(WFOSTestData.wfosRedDetectorPrefix))

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
      //********************************************************************

      //spawn wfos sequencer
      agentClient.spawnSequenceComponent(seqComponentName, Some(sequencerScriptSha)).futureValue

      seqCompLoc = locationService.find(testSeqCompConnection).futureValue

      seqCompLoc.isDefined shouldBe true

      val sequencerResponse = sequenceComponentUtil.loadScript(Subsystem.WFOS, obsMode, seqCompLoc.get).futureValue
      sequencerResponse.rightValue shouldBe a[Started]

      //********************************************************************


      val sequencerApi = sequencerClient(Subsystem.WFOS, obsMode)

      val initialSubmitRes = sequencerApi.submit(WFOSTestData.sequence).futureValue
      initialSubmitRes shouldBe a[CommandResponse.Started]

      //assert events for setupAcquisition
      assertBlueFilterPosition(wfosBlueFilterTestProbe, "g'")

      //assert events for acquisitionExposure
      assertDetectorEvents(wfosBlueDetectorTestProbe, "/tmp", ExposureId("2020A-001-123-IRIS-IMG-DRK1-0023"))

      //assert events for setupObservation
      assertBlueFilterPosition(wfosBlueFilterTestProbe, "fused-silica")
      assertRedFilterPosition(wfosRedFilterTestProbe, "z'")

      //assert events for singleExposure
      assertDetectorEvents(wfosBlueDetectorTestProbe, "/tmp", ExposureId("2020A-001-123-IRIS-IMG-DRK1-0023"))
      assertDetectorEvents(wfosRedDetectorTestProbe, "/tmp", ExposureId("2020A-001-123-IRIS-IMG-DRK1-0023"))
    }
  }

  private def assertBlueFilterPosition(testProbe: TestProbe[Event], target: String) = {
    eventually {
      val event = testProbe.expectMessageType[SystemEvent]
      event(WFOSTestData.blueFilterDemandPositionKey).head.name shouldBe target
      event(WFOSTestData.blueFilterCurrentPositionKey).head.name shouldBe target
      event(WFOSTestData.filterDarkKey).head shouldBe false
    }
  }

  private def assertRedFilterPosition(testProbe: TestProbe[Event], target: String) = {
    eventually {
      val event = testProbe.expectMessageType[SystemEvent]
      event(WFOSTestData.redFilterDemandPositionKey).head.name shouldBe target
      event(WFOSTestData.redFilterCurrentPositionKey).head.name shouldBe target
      event(WFOSTestData.filterDarkKey).head shouldBe false
    }
  }


  private def assertDetectorEvents(testProbe: TestProbe[Event], directory: String, exposureId: ExposureId) = {
    val filename = s"$directory/$exposureId.fits"

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name === ObserveEventNames.ExposureStart.name
      ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name === ObserveEventNames.ExposureEnd.name
      ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name === ObserveEventNames.DataWriteStart.name
      event(ObserveEventKeys.filename).head shouldBe filename
      ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name === ObserveEventNames.DataWriteEnd.name
      event(ObserveEventKeys.filename).head shouldBe filename
      ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
    }

  }
}

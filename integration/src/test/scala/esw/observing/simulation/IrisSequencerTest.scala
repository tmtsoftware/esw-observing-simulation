package esw.observing.simulation

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.commands.CommandResponse.Started
import csw.params.core.models.ExposureId
import csw.params.events._
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.CSWService.EventServer
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.models.Killed
import esw.commons.utils.files.FileUtils
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.MachineAgent

import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

class IrisSequencerTest extends EswTestKit(EventServer, MachineAgent) {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(1.minute, 100.millis)

  private val obsMode                         = ObsMode("IRIS_IFSOnly")
  private val agentConnection: AkkaConnection = AkkaConnection(ComponentId(agentSettings.prefix, ComponentType.Machine))
  private val testSeqCompConnection = AkkaConnection(
    ComponentId(Prefix(agentSettings.prefix.subsystem, "testComponent"), ComponentType.SequenceComponent)
  )

  private lazy val agentLoc    = locationService.find(agentConnection).futureValue
  private lazy val agentClient = new AgentClient(agentLoc.get)

  "IrisSequencer" must {
    "do something" in {
      val containerConfPath = Paths.get(ClassLoader.getSystemResource("IrisIfsContainer.conf").toURI)

      val hostConfigStr =
        s"""
           |containers: [
           |  {
           |    orgName: "com.github.tmtsoftware.esw-observing-simulation"
           |    deployModule: "iris-irisdeploy"
           |    appName: "iris.irisdeploy.IrisContainerCmdApp"
           |    version: "b6cd004"
           |    mode: "Container"
           |    configFilePath: "$containerConfPath"
           |    configFileLocation: "Local"
           |  }
           |]
           |""".stripMargin
      val hostConfigFilePath = FileUtils.createTempConfFile("hostConfig.conf", hostConfigStr)

      //spawn the iris container
      agentClient.spawnContainers(hostConfigFilePath.toString, isConfigLocal = true).futureValue

      Thread.sleep(10000)
      val containerLocation: AkkaLocation = locationService.resolve(TestData.irisContainerConnection, 5.seconds).futureValue.value

      locationService
        .resolve(AkkaConnection(ComponentId(TestData.imagerFilterPrefix, ComponentType.Assembly)), 5.seconds)
        .futureValue
        .value
      //********************************************************************

      //spawn iris sequencer
      agentClient.spawnSequenceComponent("testComponent", Some("aad9d5a")).futureValue

      val seqCompLoc = locationService.find(testSeqCompConnection).futureValue

      loadScript(seqCompLoc.get, Subsystem.IRIS, obsMode)
      //********************************************************************

      val imagerFilterTestProbe = createTestProbe(Set(TestData.imagerFilterPositionEventKey))
      val ifsResTestProbe       = createTestProbe(Set(TestData.IfsResPositionEventKey))
      val ifsScaleTestProbe     = createTestProbe(Set(TestData.IfsScaleEventKey))

      val imagerAdcTestProbe = createTestProbe(
        Set(
          TestData.ImagerADCStateEventKey,
          TestData.ImagerADCTargetEventKey,
          TestData.ImagerADCRetractEventKey,
          TestData.ImagerADCCurrentEventKey
        )
      )

//      val ifsDetectorTestProbe = createTestProbe(TestData.detectorObsEvents(TestData.IfsDetectorPrefix))

      val sequencerApi = sequencerClient(Subsystem.IRIS, obsMode)

      val initialSubmitRes = sequencerApi.submit(TestData.sequence).futureValue
      initialSubmitRes shouldBe a[Started]

      assertImagerFilterPosition(imagerFilterTestProbe)
      assertIfsResPosition(ifsResTestProbe)
      assertIfsScalePosition(ifsScaleTestProbe)
      assertAdcPrism(imagerAdcTestProbe)

//      assertDetectorEvents(ifsDetectorTestProbe, "/tmp", ExposureId("2020A-001-123-IRIS-IMG-DRK1-0023"))

      // kill the spawned container
      agentClient.killComponent(containerLocation).futureValue shouldBe Killed

      // kill the spawned sequence comp and sequencer
      agentClient.killComponent(seqCompLoc.get).futureValue shouldBe Killed
    }
  }

  private def assertIfsScalePosition(testProbe: TestProbe[Event]) = {
    eventually {
      val event1 = testProbe.expectMessageType[SystemEvent]
      event1(TestData.IfsScaleTargetPositionKey).head.name shouldBe "9"
      event1(TestData.IfsScaleCurrentPositionKey).head.name shouldBe "9"
    }
  }

  private def assertIfsResPosition(testProbe: TestProbe[Event]) = {
    eventually {
      val event1 = testProbe.expectMessageType[SystemEvent]
      event1(TestData.IfsResTargetPositionKey).head.name shouldBe "4000-H"
      event1(TestData.IfsResCurrentPositionKey).head.name shouldBe "4000-Y"
    }

    eventually {
      val event2 = testProbe.expectMessageType[SystemEvent]
      event2(TestData.IfsResTargetPositionKey).head.name shouldBe "4000-H"
      event2(TestData.IfsResCurrentPositionKey).head.name shouldBe "4000-J"
    }

    eventually {
      val event3 = testProbe.expectMessageType[SystemEvent]
      event3(TestData.IfsResTargetPositionKey).head.name shouldBe "4000-H"
      event3(TestData.IfsResCurrentPositionKey).head.name shouldBe "4000-H"
    }
  }

  private def assertImagerFilterPosition(testProbe: TestProbe[Event]) = {
    eventually {
      val event1 = testProbe.expectMessageType[SystemEvent]
      event1(TestData.ImagerFilterDemandPositionKey).head.name shouldBe "H"
      event1(TestData.ImagerFilterCurrentPositionKey).head.name shouldBe "Y"
      event1(TestData.ImagerFilterDarkKey).head shouldBe true
    }

    eventually {
      val event2 = testProbe.expectMessageType[SystemEvent]
      event2(TestData.ImagerFilterDemandPositionKey).head.name shouldBe "H"
      event2(TestData.ImagerFilterCurrentPositionKey).head.name shouldBe "J"
      event2(TestData.ImagerFilterDarkKey).head shouldBe true
    }

    eventually {
      val event3 = testProbe.expectMessageType[SystemEvent]
      event3(TestData.ImagerFilterDemandPositionKey).head.name shouldBe "H"
      event3(TestData.ImagerFilterCurrentPositionKey).head.name shouldBe "H"
      event3(TestData.ImagerFilterDarkKey).head shouldBe false
    }
  }

  private def assertAdcPrism(testProbe: TestProbe[Event]) = {

    // initially prism is stopped & on target
    val currentEvent      = testProbe.expectMessageType[SystemEvent]
    val prismCurrentState = currentEvent(TestData.adcPrismStateKey).head.name
    val isOnTarget        = currentEvent(TestData.adcPrismOnTargetKey).head
    prismCurrentState shouldBe "STOPPED"
    isOnTarget shouldBe true

    eventually {
      val goingInEvent = testProbe.expectMessageType[SystemEvent]
      goingInEvent(TestData.adcPrismRetractKey).head.name shouldBe "IN"
    }

    //verify targetAngle is set to 40.0
    eventually {
      val targetEvent = testProbe.expectMessageType[SystemEvent]
      targetEvent.eventName shouldBe TestData.ImagerADCTargetEventName
      targetEvent(TestData.adcPrismAngleKey).head shouldBe 40.0
    }

    //verify whether prism has started moving
    eventually {
      val movingEvent = testProbe.expectMessageType[SystemEvent]
      movingEvent.eventName shouldBe TestData.ImagerADCStateEventName
      movingEvent(TestData.adcPrismStateKey).head.name shouldBe "MOVING"
      movingEvent(TestData.adcPrismOnTargetKey).head shouldBe false
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

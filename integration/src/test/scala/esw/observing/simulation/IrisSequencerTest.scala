package esw.observing.simulation

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.framework.deploy.containercmd.ContainerCmd
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.commands.CommandResponse
import esw.sm.api.protocol.StartSequencerResponse.Started
import csw.params.core.models.ExposureId
import csw.params.events._
import csw.prefix.models.Subsystem.IRIS
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.CSWService.EventServer
import esw.agent.akka.client.AgentClient
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.MachineAgent
import esw.sm.impl.utils.{SequenceComponentAllocator, SequenceComponentUtil}

import java.io.Closeable
import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

class IrisSequencerTest extends EswTestKit(EventServer, MachineAgent) {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(1.minute, 100.millis)

  private val obsMode                         = ObsMode("IRIS_ImagerAndIFS")
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

  "IrisSequencer" must {
    "handle the submitted sequence | ESW-551" in {

      val containerConfPath = Paths.get(getClass.getResource("/IrisContainer.conf").toURI)

      //spawn the iris container
      containerCmd = Some(ContainerCmd.start("iris_container_cmd_app", IRIS, List("--local", containerConfPath.toString).toArray))

      val containerLocation: Option[AkkaLocation] =
        locationService.resolve(TestData.irisContainerConnection, 15.seconds).futureValue
      containerLocation.isDefined shouldBe true

      locationService
        .resolve(AkkaConnection(ComponentId(TestData.imagerFilterPrefix, ComponentType.Assembly)), 5.seconds)
        .futureValue
        .value
      //********************************************************************

      //spawn iris sequencer
      agentClient.spawnSequenceComponent(seqComponentName, Some(TestData.sequencerScriptSha)).futureValue

      seqCompLoc = locationService.find(testSeqCompConnection).futureValue

      seqCompLoc.isDefined shouldBe true

      val sequencerResponse = sequenceComponentUtil.loadScript(Subsystem.IRIS, obsMode, seqCompLoc.get).futureValue
      sequencerResponse.rightValue shouldBe a[Started]

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

      val ifsDetectorTestProbe    = createTestProbe(TestData.detectorObsEvents(TestData.IfsDetectorPrefix))
      val imagerDetectorTestProbe = createTestProbe(TestData.detectorObsEvents(TestData.ImagerDetectorPrefix))

      val sequencerApi = sequencerClient(Subsystem.IRIS, obsMode)

      val initialSubmitRes = sequencerApi.submit(TestData.irisSequence).futureValue
      initialSubmitRes shouldBe a[CommandResponse.Started]

      //sequence : setupAcquisition, acquisitionExposure, setupObservation, singleExposure
      assertAdcInitialized(imagerAdcTestProbe)

      //assert events for setupAcquisition
      assertImagerFilterPosition(imagerFilterTestProbe, "Ks", "H")
      assertAdcPrism(imagerAdcTestProbe, acquisition = true, 40.0)

      //assert events for acquisitionExposure
      assertDetectorEvents(imagerDetectorTestProbe, "/tmp", ExposureId("2020A-001-123-IRIS-IMG-DRK1-0023"))

      //assert events for setupObservation
      assertAdcPrism(imagerAdcTestProbe, acquisition = false, 50.0)
      assertImagerFilterPosition(imagerFilterTestProbe, "CO", "H+K notch")
      assertIfsResPosition(ifsResTestProbe)
      assertIfsScalePosition(ifsScaleTestProbe)

      //assert events for singleExposure
      assertDetectorEvents(imagerDetectorTestProbe, "/tmp", ExposureId("2020A-001-123-IRIS-IMG-DRK1-0023"))
      assertDetectorEvents(ifsDetectorTestProbe, "/tmp", ExposureId("2020A-001-123-IRIS-IMG-DRK1-0023"))
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

  private def assertImagerFilterPosition(testProbe: TestProbe[Event], target: String, current: String) = {
    eventually {
      val event1 = testProbe.expectMessageType[SystemEvent]
      event1(TestData.ImagerFilterDemandPositionKey).head.name shouldBe target
      event1(TestData.ImagerFilterCurrentPositionKey).head.name shouldBe current
      event1(TestData.ImagerFilterDarkKey).head shouldBe true
    }

    eventually {
      val event2 = testProbe.expectMessageType[SystemEvent]
      event2(TestData.ImagerFilterDemandPositionKey).head.name shouldBe target
      event2(TestData.ImagerFilterCurrentPositionKey).head.name shouldBe target
      event2(TestData.ImagerFilterDarkKey).head shouldBe false
    }
  }

  private def assertAdcInitialized(testProbe: TestProbe[Event]): Unit = {
    eventually {
      val stateEvent = testProbe.expectMessageType[SystemEvent]
      stateEvent.eventName shouldBe TestData.ImagerADCStateEventName
      val prismCurrentState = stateEvent(TestData.adcPrismStateKey).head.name
      val isOnTarget        = stateEvent(TestData.adcPrismOnTargetKey).head
      prismCurrentState shouldBe "STOPPED"
      isOnTarget shouldBe true
    }
  }
  private def assertAdcPrism(testProbe: TestProbe[Event], acquisition: Boolean, targetAngle: Double): Unit = {

    // initially prism is stopped & on target
    if (acquisition) {
      eventually {
        val goingInEvent = testProbe.expectMessageType[SystemEvent]
        goingInEvent.eventName shouldBe TestData.ImagerADCRetractEventName
        goingInEvent(TestData.adcPrismRetractKey).head.name shouldBe "IN"
      }
    }

    //verify targetAngle is set
    eventually {
      val targetEvent = testProbe.expectMessageType[SystemEvent]
      targetEvent.eventName shouldBe TestData.ImagerADCTargetEventName
      targetEvent(TestData.adcPrismAngleKey).head shouldBe targetAngle
    }

    //verify whether prism has started moving
    eventually {
      val movingEvent = testProbe.expectMessageType[SystemEvent]
      movingEvent.eventName shouldBe TestData.ImagerADCStateEventName
      movingEvent(TestData.adcPrismStateKey).head.name shouldBe "MOVING"
      movingEvent(TestData.adcPrismOnTargetKey).head shouldBe false
    }

    //verify current angle is changing
    eventually {
      val currentEvent = testProbe.expectMessageType[SystemEvent]
      currentEvent.eventName shouldBe TestData.ImagerADCCurrentEventName
      currentEvent(TestData.adcPrismAngleKey).head should be > 0.0
      currentEvent(TestData.adcPrismAngleErrorKey).head should be > 0.0
    }

    eventually {
      val movingEvent = testProbe.expectMessageType[SystemEvent]
      movingEvent.eventName shouldBe TestData.ImagerADCStateEventName
      movingEvent(TestData.adcPrismStateKey).head.name shouldBe "MOVING"
      movingEvent(TestData.adcPrismOnTargetKey).head shouldBe true
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

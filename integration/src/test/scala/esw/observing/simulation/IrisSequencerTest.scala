package esw.observing.simulation

import akka.actor.testkit.typed.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.commands.CommandResponse
import csw.params.core.models.Coords.{AltAzCoord, BASE}
import csw.params.core.models.{Angle, ExposureId}
import csw.params.events._
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.CSWService.EventServer
import esw.agent.akka.app.process.{ProcessExecutor, ProcessOutput}
import esw.agent.akka.client.AgentClient
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.MachineAgent
import esw.sm.api.protocol.StartSequencerResponse.Started
import esw.sm.impl.utils.{SequenceComponentAllocator, SequenceComponentUtil}
import iris.imageradc.events.TCSEvents

import scala.concurrent.duration.DurationInt

class IrisSequencerTest extends EswTestKit(EventServer, MachineAgent) {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(2.minute, 100.millis)
  lazy val processOutput                               = new ProcessOutput()
  implicit lazy val log: Logger                        = new LoggerFactory(agentSettings.prefix).getLogger
  lazy val processExecutor                             = new ProcessExecutor(processOutput)
  private val obsMode                                  = ObsMode("IRIS_ImagerAndIFS")
  private val seqComponentName                         = "testComponent"
  private val agentConnection: AkkaConnection          = AkkaConnection(ComponentId(agentSettings.prefix, ComponentType.Machine))
  private val testSeqCompConnection = AkkaConnection(
    ComponentId(Prefix(agentSettings.prefix.subsystem, seqComponentName), ComponentType.SequenceComponent)
  )

  private lazy val agentLoc    = locationService.find(agentConnection).futureValue
  private lazy val agentClient = new AgentClient(agentLoc.get)

  private val locationServiceUtil              = new LocationServiceUtil(locationService)
  private val sequenceComponentUtil            = new SequenceComponentUtil(locationServiceUtil, new SequenceComponentAllocator())
  private var seqCompLoc: Option[AkkaLocation] = None

  override def afterAll(): Unit = {
    seqCompLoc.map(seqCompLocation => agentClient.killComponent(seqCompLocation).futureValue)
    super.afterAll()
  }

  "IrisSequencer" must {
    "handle the submitted sequence | ESW-551, ESW-566" in {

      // spawn the iris container
      frameworkTestKit.spawnContainer(ConfigFactory.load("IrisContainer.conf"))

      val containerLocation: Option[AkkaLocation] =
        locationService.resolve(TestData.irisContainerConnection, 15.seconds).futureValue
      containerLocation.isDefined shouldBe true

      locationService
        .resolve(AkkaConnection(ComponentId(TestData.imagerFilterPrefix, ComponentType.Assembly)), 5.seconds)
        .futureValue
        .value
      // ********************************************************************

      // spawn iris sequencer
      agentClient.spawnSequenceComponent(seqComponentName, Some(ScriptVersion.value)).futureValue

      seqCompLoc = locationService.find(testSeqCompConnection).futureValue

      seqCompLoc.isDefined shouldBe true

      val sequencerResponse = sequenceComponentUtil.loadScript(Subsystem.IRIS, obsMode, None, seqCompLoc.get).futureValue
      sequencerResponse.rightValue shouldBe a[Started]

      // ********************************************************************

      val imagerFilterTestProbe = createTestProbe(Set(TestData.imagerFilterPositionEventKey))
      val ifsResTestProbe       = createTestProbe(Set(TestData.IfsResPositionEventKey))
      val ifsScaleTestProbe     = createTestProbe(Set(TestData.IfsScaleEventKey))

      val imagerAdcTestProbe = createTestProbe(
        Set(
          TestData.ImagerADCStateEventKey,
          TestData.ImagerADCRetractEventKey,
          TestData.ImagerADCCurrentEventKey
        )
      )

      val ifsDetectorTestProbe    = createTestProbe(TestData.detectorObsEvents(TestData.IfsDetectorPrefix))
      val imagerDetectorTestProbe = createTestProbe(TestData.detectorObsEvents(TestData.ImagerDetectorPrefix))

      import Angle._
      eventService.defaultPublisher.publish(TCSEvents.make(AltAzCoord(BASE, 50.degree, 20.degree))) // 90 - 50(alt) = target (40)

      val sequencerApi     = sequencerClient(Subsystem.IRIS, obsMode)
      val initialSubmitRes = sequencerApi.submit(TestData.irisSequence).futureValue
      initialSubmitRes shouldBe a[CommandResponse.Started]
      // sequence : setupAcquisition, acquisitionExposure, setupObservation, singleExposure
      assertAdcInitialized(imagerAdcTestProbe)
      // assert events for setupAcquisition
      assertImagerFilterPosition(imagerFilterTestProbe, "Ks", "H")

      assertAdcPrismInSetupAcquisition(imagerAdcTestProbe, 10.0, 40.0)

      // assert events for acquisitionExposure
      assertDetectorEvents(imagerDetectorTestProbe, "/tmp", ExposureId("2020A-001-123-IRIS-IMG-DRK1-0023"))
      eventService.defaultPublisher.publish(TCSEvents.make(AltAzCoord(BASE, 40.degree, 20.degree))) // 90-40 = target (50)
      // assert events for setupObservation
      assertAdcPrismInSetupObservation(imagerAdcTestProbe, 45.0, 50.0)
      assertImagerFilterPosition(imagerFilterTestProbe, "CO", "H+K notch")
      assertIfsResPosition(ifsResTestProbe)
      assertIfsScalePosition(ifsScaleTestProbe)

      // assert events for singleExposure
      assertDetectorEvents(imagerDetectorTestProbe, "/tmp", ExposureId("2020A-001-123-IRIS-IMG-DRK1-0023"))
      assertDetectorEvents(ifsDetectorTestProbe, "/tmp", ExposureId("2020A-001-123-IRIS-IMG-DRK1-0023"))

      assertAdcShutdown(imagerAdcTestProbe)
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
  private def assertAdcShutdown(testProbe: TestProbe[Event]): Unit = {
    eventually {
      val stateEvent = testProbe.expectMessageType[SystemEvent]
      stateEvent.eventName shouldBe TestData.ImagerADCStateEventName
      val prismCurrentState = stateEvent(TestData.adcPrismStateKey).head.name
      val isOnTarget        = stateEvent(TestData.adcPrismOnTargetKey).head
      prismCurrentState shouldBe "STOPPED"
      isOnTarget shouldBe true
    }

    eventually {
      val retractEvent = testProbe.expectMessageType[SystemEvent]
      retractEvent.eventName shouldBe TestData.ImagerADCRetractEventName
      val prismCurrentState = retractEvent(TestData.adcPrismRetractKey).head.name
      prismCurrentState shouldBe "OUT"
    }
  }

  private def assertAdcPrismInSetupAcquisition(testProbe: TestProbe[Event], currentAngle: Double, targetAngle: Double): Unit = {

    // initially prism is stopped & on target
    eventually {
      val goingInEvent = testProbe.expectMessageType[SystemEvent]
      goingInEvent.eventName shouldBe TestData.ImagerADCRetractEventName
      goingInEvent(TestData.adcPrismRetractKey).head.name shouldBe "IN"
    }

    // verify whether prism has started moving
    eventually {
      val movingEvent = testProbe.expectMessageType[SystemEvent]
      movingEvent.eventName shouldBe TestData.ImagerADCStateEventName
      movingEvent(TestData.adcPrismStateKey).head.name shouldBe "FOLLOWING"
      movingEvent(TestData.adcPrismOnTargetKey).head shouldBe false
    }

    // verify prism angle's
    eventually {
      val currentEvent = testProbe.expectMessageType[SystemEvent]
      currentEvent.eventName shouldBe TestData.ImagerADCCurrentEventName
      currentEvent(TestData.adcPrismAngleKey).head should be > currentAngle
      currentEvent(TestData.adcPrismTargetAngleKey).head shouldBe targetAngle
      currentEvent(TestData.adcPrismAngleErrorKey).head should be > 0.0
    }

    eventually {
      val movingEvent = testProbe.expectMessageType[SystemEvent]
      movingEvent.eventName shouldBe TestData.ImagerADCStateEventName
      movingEvent(TestData.adcPrismStateKey).head.name shouldBe "FOLLOWING"
      movingEvent(TestData.adcPrismOnTargetKey).head shouldBe true
    }

  }

  private def assertAdcPrismInSetupObservation(testProbe: TestProbe[Event], currentAngle: Double, targetAngle: Double): Unit = {

    // verify whether prism has started moving
    eventually {
      val movingEvent = testProbe.expectMessageType[SystemEvent]
      movingEvent.eventName shouldBe TestData.ImagerADCStateEventName
      movingEvent(TestData.adcPrismStateKey).head.name shouldBe "FOLLOWING"
      movingEvent(TestData.adcPrismOnTargetKey).head shouldBe false
    }

    // verify prism angle's
    eventually {
      val currentEvent = testProbe.expectMessageType[SystemEvent]
      currentEvent.eventName shouldBe TestData.ImagerADCCurrentEventName
      currentEvent(TestData.adcPrismAngleKey).head should be > currentAngle
      currentEvent(TestData.adcPrismTargetAngleKey).head shouldBe targetAngle
      currentEvent(TestData.adcPrismAngleErrorKey).head should be > 0.0
    }

    eventually {
      val movingEvent = testProbe.expectMessageType[SystemEvent]
      movingEvent.eventName shouldBe TestData.ImagerADCStateEventName
      movingEvent(TestData.adcPrismStateKey).head.name shouldBe "FOLLOWING"
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

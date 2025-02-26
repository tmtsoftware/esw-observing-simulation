package esw.observing.simulation

import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{PekkoLocation, ComponentId, ComponentType}
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.commands.CommandResponse
import csw.params.events.ObserveEventNames.{ExposureEnd, ExposureStart}
import csw.params.events._
import csw.prefix.models.Subsystem.Container
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.CSWService.EventServer
import esw.agent.pekko.app.process.{ProcessExecutor, ProcessOutput}
import esw.agent.pekko.client.AgentClient
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.MachineAgent
import esw.sm.api.protocol.StartSequencerResponse.Started
import esw.sm.impl.utils.{SequenceComponentAllocator, SequenceComponentUtil}

import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

class EswIrisSequencerTest extends EswTestKit(EventServer, MachineAgent) {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(2.minute, 100.millis)
  lazy val processOutput                               = new ProcessOutput()
  implicit lazy val log: Logger                        = new LoggerFactory(agentSettings.prefix).getLogger
  lazy val processExecutor                             = new ProcessExecutor(processOutput)
  private val obsMode                                  = ObsMode("IRIS_ImagerAndIFS")
  private val seqComponentName1                        = "testComponent1"
  private val seqComponentName2                        = "testComponent2"
  private val seqComponentName3                        = "testComponent3"
  private val agentConnection: PekkoConnection         = PekkoConnection(ComponentId(agentSettings.prefix, ComponentType.Machine))
  private val testSeqCompConnection1 = PekkoConnection(
    ComponentId(Prefix(agentSettings.prefix.subsystem, seqComponentName1), ComponentType.SequenceComponent)
  )
  private val testSeqCompConnection2 = PekkoConnection(
    ComponentId(Prefix(agentSettings.prefix.subsystem, seqComponentName2), ComponentType.SequenceComponent)
  )
  private val testSeqCompConnection3 = PekkoConnection(
    ComponentId(Prefix(agentSettings.prefix.subsystem, seqComponentName3), ComponentType.SequenceComponent)
  )

  private lazy val agentLoc    = locationService.find(agentConnection).futureValue
  private lazy val agentClient = new AgentClient(agentLoc.get)

  private val locationServiceUtil                 = new LocationServiceUtil(locationService)
  private val sequenceComponentUtil               = new SequenceComponentUtil(locationServiceUtil, new SequenceComponentAllocator())
  private var seqComp1Loc: Option[PekkoLocation]  = None
  private var seqComp2Loc: Option[PekkoLocation]  = None
  private var seqComp3Loc: Option[PekkoLocation]  = None
  private var containerLoc: Option[PekkoLocation] = None

  override def afterAll(): Unit = {
    seqComp1Loc.map(seqCompLocation => agentClient.killComponent(seqCompLocation).futureValue)
    seqComp2Loc.map(seqCompLocation => agentClient.killComponent(seqCompLocation).futureValue)
    seqComp3Loc.map(seqCompLocation => agentClient.killComponent(seqCompLocation).futureValue)
    containerLoc.map(compLocation => agentClient.killComponent(compLocation).futureValue)

    super.afterAll()
  }

  /*
   This test submits the sequence to top level ESW sequencer for an observation, it internally submits sequences corresponding to each
   command to downstream sequencer i.e. IRIS in our case. Now, IRIS sequencer send commands to respective assemblies.
   We subscribe and validate all observe events are published by ESW sequencer and in correct order.
   */
  "Iris top level esw sequencer" must {
    "handle the submitted sequence | ESW-554, ESW-82, ESW-570, ESW-589" in {

      // spawn the iris container
      frameworkTestKit.spawnContainer(ConfigFactory.load("IrisContainer.conf"))

      val containerLocation: Option[PekkoLocation] =
        locationService.resolve(TestData.irisContainerConnection, 15.seconds).futureValue
      containerLocation.isDefined shouldBe true

      locationService
        .resolve(PekkoConnection(ComponentId(TestData.imagerFilterPrefix, ComponentType.Assembly)), 5.seconds)
        .futureValue
        .value

      locationService
        .resolve(PekkoConnection(ComponentId(TestData.IfsDetectorPrefix, ComponentType.Assembly)), 5.seconds)
        .futureValue
        .value

      locationService
        .resolve(PekkoConnection(ComponentId(TestData.ImagerDetectorPrefix, ComponentType.Assembly)), 5.seconds)
        .futureValue
        .value

      locationService
        .resolve(PekkoConnection(ComponentId(TestData.ImagerADCAssemblyPrefix, ComponentType.Assembly)), 5.seconds)
        .futureValue
        .value

      val script = Paths.get(getClass.getResource("/test-setup.sh").toURI)

      processExecutor
        .runCommand(List(script.toString), Prefix(Container, "TcsContainer"))
        .rightValue
      // wait for tcs-zip to download
      Thread.sleep(20000)
      containerLoc = locationService.resolve(TestData.tcsContainerConnection, 20.seconds).futureValue
      containerLoc.isDefined shouldBe true

      locationService
        .resolve(TestData.tcsPkAssemblyConnection, 5.seconds)
        .futureValue
        .isDefined shouldBe true

      locationService
        .resolve(TestData.tcsMcsAssemblyConnection, 5.seconds)
        .futureValue
        .isDefined shouldBe true

      locationService
        .resolve(TestData.tcsEncAssemblyConnection, 5.seconds)
        .futureValue
        .isDefined shouldBe true

      // ********************************************************************

      // spawn esw and iris sequencer
      agentClient.spawnSequenceComponent(seqComponentName1, Some(ScriptVersion.value)).futureValue
      agentClient.spawnSequenceComponent(seqComponentName2, Some(ScriptVersion.value)).futureValue
      agentClient.spawnSequenceComponent(seqComponentName3, Some(ScriptVersion.value)).futureValue

      seqComp1Loc = locationService.find(testSeqCompConnection1).futureValue
      seqComp2Loc = locationService.find(testSeqCompConnection2).futureValue
      seqComp3Loc = locationService.find(testSeqCompConnection3).futureValue

      seqComp1Loc.isDefined shouldBe true
      seqComp2Loc.isDefined shouldBe true
      seqComp3Loc.isDefined shouldBe true

      val eswSequencerResponse = sequenceComponentUtil.loadScript(Subsystem.ESW, obsMode, None, seqComp1Loc.get).futureValue
      eswSequencerResponse.rightValue shouldBe a[Started]

      val irisSequencerResponse = sequenceComponentUtil.loadScript(Subsystem.IRIS, obsMode, None, seqComp2Loc.get).futureValue
      irisSequencerResponse.rightValue shouldBe a[Started]

      val tcsSequencerResponse = sequenceComponentUtil.loadScript(Subsystem.TCS, obsMode, None, seqComp3Loc.get).futureValue
      tcsSequencerResponse.rightValue shouldBe a[Started]

      // ********************************************************************

      val dmsConsumerProbe = createTestProbe(
        TestData.observeEventKeys
          ++ TestData.detectorObsEvents(Prefix("IRIS.ifs.detector"))
          ++ Set(TestData.encCurrentPositionEventKey)
          ++ Set(TestData.offsetStartEventKey, TestData.offsetEndEventKey)
      )

      val sequencerApi = sequencerClient(Subsystem.ESW, obsMode)

      val initialSubmitRes = sequencerApi.submit(TestData.eswSequence).futureValue
      initialSubmitRes shouldBe a[CommandResponse.Started]

      assertObserveEvents(dmsConsumerProbe)
    }
  }

  // ESW-82
  private def assertObserveEvents(testProbe: TestProbe[Event]) = {
    // sequence : eswObservationStart,preset,coarseAcquisition,fineAcquisition,setupObservation,observe,observationEnd

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.ObservationStart.name
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.PresetStart.name
    }

    eventually {
      val event = testProbe.expectMessageType[SystemEvent]
      event.eventName.name shouldBe "CurrentPosition"
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.PresetEnd.name
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.GuidestarAcqStart.name
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.GuidestarAcqEnd.name
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.ScitargetAcqStart.name
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.ScitargetAcqEnd.name
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.OffsetStart.name
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.OffsetEnd.name
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.ObserveStart.name
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ExposureStart.name
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ExposureEnd.name
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.ObserveEnd.name
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.ObservationEnd.name
    }
  }

}

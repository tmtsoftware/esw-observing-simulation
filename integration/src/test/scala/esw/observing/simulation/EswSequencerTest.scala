package esw.observing.simulation

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.framework.deploy.containercmd.ContainerCmd
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.commands.CommandResponse
import csw.params.events.ObserveEventNames.{ExposureEnd, ExposureStart}
import csw.params.events._
import csw.prefix.models.Subsystem.IRIS
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

class EswSequencerTest extends EswTestKit(EventServer, MachineAgent) {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(1.minute, 100.millis)

  private val obsMode                         = ObsMode("IRIS_ImagerAndIFS")
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
   command to downstream sequencer i.e. IRIS in our case. Now, IRIS sequencer send commands to respective assemblies.
   We subscribe and validate all observe events are published by ESW sequencer and in correct order.
   */
  "EswSequencer" must {
    "handle the submitted sequence | ESW-554" in {

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

      locationService
        .resolve(AkkaConnection(ComponentId(TestData.IfsDetectorPrefix, ComponentType.Assembly)), 5.seconds)
        .futureValue
        .value

      locationService
        .resolve(AkkaConnection(ComponentId(TestData.ImagerDetectorPrefix, ComponentType.Assembly)), 5.seconds)
        .futureValue
        .value

      locationService
        .resolve(AkkaConnection(ComponentId(TestData.ImagerADCAssemblyPrefix, ComponentType.Assembly)), 5.seconds)
        .futureValue
        .value

      //********************************************************************

      //spawn esw and iris sequencer
      agentClient.spawnSequenceComponent(seqComponentName1, Some(TestData.sequencerScriptSha)).futureValue
      agentClient.spawnSequenceComponent(seqComponentName2, Some(TestData.sequencerScriptSha)).futureValue

      seqComp1Loc = locationService.find(testSeqCompConnection1).futureValue
      seqComp2Loc = locationService.find(testSeqCompConnection2).futureValue

      seqComp1Loc.isDefined shouldBe true
      seqComp2Loc.isDefined shouldBe true

      val eswSequencerResponse = sequenceComponentUtil.loadScript(Subsystem.ESW, obsMode, seqComp1Loc.get).futureValue
      eswSequencerResponse.rightValue shouldBe a[Started]

      val irisSequencerResponse = sequenceComponentUtil.loadScript(Subsystem.IRIS, obsMode, seqComp2Loc.get).futureValue
      irisSequencerResponse.rightValue shouldBe a[Started]

      //********************************************************************

      val dmsConsumerProbe = createTestProbe(TestData.observeEventKeys ++ TestData.detectorObsEvents(Prefix("IRIS.ifs.detector")))

      val sequencerApi = sequencerClient(Subsystem.ESW, obsMode)

      val initialSubmitRes = sequencerApi.submit(TestData.eswSequence).futureValue
      initialSubmitRes shouldBe a[CommandResponse.Started]

      assertObserveEvents(dmsConsumerProbe)
    }
  }

  private def assertObserveEvents(testProbe: TestProbe[Event]) = {
    //sequence : eswObservationStart,preset,coarseAcquisition,fineAcquisition,setupObservation,observe,observationEnd

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.ObservationStart.name
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name shouldBe ObserveEventNames.PresetStart.name
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

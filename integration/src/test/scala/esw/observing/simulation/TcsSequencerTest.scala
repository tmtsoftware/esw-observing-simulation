package esw.observing.simulation

import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{PekkoLocation, ComponentId, ComponentType}
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.commands.CommandResponse
import csw.params.core.models.Angle
import csw.params.core.models.Angle.double2angle
import csw.params.events.{ObserveEvent, SystemEvent}
import csw.prefix.models.Subsystem.Container
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.CSWService.EventServer
import esw.agent.pekko.app.process.{ProcessExecutor, ProcessOutput}
import esw.agent.pekko.client.AgentClient
import esw.commons.utils.location.LocationServiceUtil
import esw.observing.simulation.TestData._
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.MachineAgent
import esw.sm.api.protocol.StartSequencerResponse.Started
import esw.sm.impl.utils.{SequenceComponentAllocator, SequenceComponentUtil}

import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

class TcsSequencerTest extends EswTestKit(EventServer, MachineAgent) {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(1.minute, 200.millis)
  lazy val processOutput                               = new ProcessOutput()
  implicit lazy val log: Logger                        = new LoggerFactory(agentSettings.prefix).getLogger
  lazy val processExecutor                             = new ProcessExecutor(processOutput)
  private val obsMode                                  = ObsMode("IRIS_ImagerAndIFS")
  private val seqComponentName                         = "testComponent"
  private val agentConnection: PekkoConnection          = PekkoConnection(ComponentId(agentSettings.prefix, ComponentType.Machine))
  private val testSeqCompConnection = PekkoConnection(
    ComponentId(Prefix(agentSettings.prefix.subsystem, seqComponentName), ComponentType.SequenceComponent)
  )

  private lazy val agentLoc    = locationService.find(agentConnection).futureValue
  private lazy val agentClient = new AgentClient(agentLoc.get)

  private val locationServiceUtil                = new LocationServiceUtil(locationService)
  private val sequenceComponentUtil              = new SequenceComponentUtil(locationServiceUtil, new SequenceComponentAllocator())
  private var seqCompLoc: Option[PekkoLocation]   = None
  private var containerLoc: Option[PekkoLocation] = None

  override def afterAll(): Unit = {
    seqCompLoc.map(seqCompLocation => agentClient.killComponent(seqCompLocation).futureValue)
    containerLoc.map(compLocation => agentClient.killComponent(compLocation).futureValue)
    super.afterAll()
  }

  "TcsSequencer" must {
    "handle the submitted sequence | ESW-569, ESW-589" in {

      val script = Paths.get(getClass.getResource("/test-setup.sh").toURI)

      processExecutor.runCommand(List(script.toString), Prefix(Container, "TcsContainer")).rightValue
      // wait for tcs-zip to download
      Thread.sleep(20000)
      containerLoc = locationService.resolve(TestData.tcsContainerConnection, 15.seconds).futureValue
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

      // spawn iris sequencer
      agentClient.spawnSequenceComponent(seqComponentName, Some(ScriptVersion.value)).futureValue

      seqCompLoc = locationService.find(testSeqCompConnection).futureValue

      seqCompLoc.isDefined shouldBe true

      val sequencerResponse = sequenceComponentUtil.loadScript(Subsystem.TCS, obsMode, None, seqCompLoc.get).futureValue
      sequencerResponse.rightValue shouldBe a[Started]

      // ********************************************************************

      val pkAssemblyTestProbe   = createTestProbe(Set(TestData.mcsDemandPositionEventKey, TestData.encCurrentPositionEventKey))
      val tcsSequencerTestProbe = createTestProbe(Set(TestData.offsetStartEventKey, TestData.offsetEndEventKey))

      val sequencerApi     = sequencerClient(Subsystem.TCS, obsMode)
      val initialSubmitRes = sequencerApi.submit(TestData.tcsSequence).futureValue
      initialSubmitRes shouldBe a[CommandResponse.Started]
      // sequence : preset, setupObservation

      // Assert MountPosition for SlewToTarget command
      eventually {
        val event = pkAssemblyTestProbe.expectMessageType[SystemEvent]
        event.eventName.name shouldBe "MountPosition"
        assertMountPositionError(event, 5.0)
      }

      // Assert CurrentPosition for SlewToTarget command
      eventually {
        val event = pkAssemblyTestProbe.expectMessageType[SystemEvent]
        event.eventName.name shouldBe "CurrentPosition"
        assertCapAndBaseError(event)
      }

      eventually {
        val event = tcsSequencerTestProbe.expectMessageType[ObserveEvent]
        event.eventName.name shouldBe "ObserveEvent.OffsetStart"
      }
      // Assert MountPosition for SetOffset command
      eventually {
        val event = pkAssemblyTestProbe.expectMessageType[SystemEvent]
        event.eventName.name shouldBe "MountPosition"
        assertMountPositionError(event, 0.5)
      }

      eventually {
        val event = tcsSequencerTestProbe.expectMessageType[ObserveEvent]
        event.eventName.name shouldBe "ObserveEvent.OffsetEnd"
      }

      val eventualResponse = sequencerApi.queryFinal(initialSubmitRes.runId)(20.seconds).futureValue
      eventualResponse shouldBe a[CommandResponse.Completed]

    }

    def assertMountPositionError(event: SystemEvent, tolerance: Double) = {
      val current = event(currentEqCoordKey).head
      val demand  = event(demandEqCoordKey).head
      Angle
        .distance(current.ra.toRadian, current.dec.toRadian, demand.ra.toRadian, demand.dec.toRadian)
        .radian
        .toArcSec should be < tolerance
    }

    def assertCapAndBaseError(event: SystemEvent) = {
      val baseCurrentValue = event(baseCurrentKey).head
      val capCurrentValue  = event(capCurrentKey).head
      val baseDemandValue  = event(baseDemandKey).head
      val capDemandValue   = event(capDemandKey).head

      val capError  = Math.abs(capCurrentValue.degree.toArcSec - capDemandValue.degree.toArcSec)
      val baseError = Math.abs(baseCurrentValue.degree.toArcSec - baseDemandValue.degree.toArcSec)
      capError should be < 5.0
      baseError should be < 5.0
    }
  }
}

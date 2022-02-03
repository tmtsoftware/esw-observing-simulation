package esw.observing.simulation

import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.commands.CommandResponse
import csw.params.core.models.Angle
import csw.params.events.SystemEvent
import csw.prefix.models.Subsystem.Container
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.CSWService.EventServer
import esw.agent.akka.app.process.{ProcessExecutor, ProcessOutput}
import esw.agent.akka.client.AgentClient
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
  private val agentConnection: AkkaConnection          = AkkaConnection(ComponentId(agentSettings.prefix, ComponentType.Machine))
  private val testSeqCompConnection = AkkaConnection(
    ComponentId(Prefix(agentSettings.prefix.subsystem, seqComponentName), ComponentType.SequenceComponent)
  )

  private lazy val agentLoc    = locationService.find(agentConnection).futureValue
  private lazy val agentClient = new AgentClient(agentLoc.get)

  private val locationServiceUtil                = new LocationServiceUtil(locationService)
  private val sequenceComponentUtil              = new SequenceComponentUtil(locationServiceUtil, new SequenceComponentAllocator())
  private var seqCompLoc: Option[AkkaLocation]   = None
  private var containerLoc: Option[AkkaLocation] = None

  override def afterAll(): Unit = {
    seqCompLoc.map(seqCompLocation => agentClient.killComponent(seqCompLocation).futureValue)
    containerLoc.map(compLocation => agentClient.killComponent(compLocation).futureValue)
    super.afterAll()
  }

  "TcsSequencer" must {
    "handle the submitted sequence | ESW-569" in {

      val containerConfPath = Paths.get(getClass.getResource("/TcsContainer.conf").toURI)

      val script = Paths.get(getClass.getResource("/test-setup.sh").toURI)

      processExecutor.runCommand(List(script.toString, containerConfPath.toString), Prefix(Container, "TcsContainer")).rightValue

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

      val pkAssemblyTestProbe = createTestProbe(Set(TestData.mcsDemandPositionEventKey, TestData.encCurrentPositionEventKey))

      val sequencerApi     = sequencerClient(Subsystem.TCS, obsMode)
      val initialSubmitRes = sequencerApi.submit(TestData.tcsSequence).futureValue
      initialSubmitRes shouldBe a[CommandResponse.Started]
      // sequence : preset, setupObservation

      // Assert MountPosition for SlewToTarget command
      eventually {
        val event = pkAssemblyTestProbe.expectMessageType[SystemEvent]
        event.eventName.name shouldBe "MountPosition"
        assertMountPositionError(event, 0.5)
      }

      // Assert CurrentPosition for SlewToTarget command
      eventually {
        val event = pkAssemblyTestProbe.expectMessageType[SystemEvent]
        event.eventName.name shouldBe "CurrentPosition"
        assertCapAndBaseError(event)
      }

      // Assert MountPosition for SetOffset command
      eventually {
        val event = pkAssemblyTestProbe.expectMessageType[SystemEvent]
        event.eventName.name shouldBe "MountPosition"
        assertMountPositionError(event, 0.1)
      }

      val eventualResponse = sequencerApi.queryFinal(initialSubmitRes.runId)(20.seconds).futureValue
      eventualResponse shouldBe a[CommandResponse.Completed]

    }

    def assertMountPositionError(event: SystemEvent, tolerance: Double) = {
      val current = event(currentAltAzCoordKey).head
      val demand  = event(demandAltAzCoordKey).head
      Angle.distance(current.alt.toRadian, current.az.toRadian, demand.alt.toRadian, demand.az.toRadian) should be < tolerance
    }

    def assertCapAndBaseError(event: SystemEvent) = {
      val baseCurrentValue = event(baseCurrentKey).head
      val capCurrentValue  = event(capCurrentKey).head
      val baseDemandValue  = event(baseDemandKey).head
      val capDemandValue   = event(capDemandKey).head

      val capError  = Math.abs(capCurrentValue - capDemandValue)
      val baseError = Math.abs(baseCurrentValue - baseDemandValue)
      capError should be < 0.5
      baseError should be < 0.5
    }
  }
}

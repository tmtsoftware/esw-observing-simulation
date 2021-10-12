package iris.imageradc

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import akka.util.Timeout
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.ComponentMessage
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Invalid, Started}
import csw.params.commands.Setup
import csw.params.events.{Event, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS
import csw.testkit.scaladsl.CSWService.EventServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import iris.imageradc.commands.ADCCommand
import iris.imageradc.events.PrismCurrentEvent.{ImagerADCCurrentEventKey, ImagerADCCurrentEventName, angleErrorKey}
import iris.imageradc.events.PrismRetractEvent.{ImagerADCRetractEventKey, ImagerADCRetractEventName}
import iris.imageradc.events.PrismStateEvent.{ImagerADCStateEventKey, ImagerADCStateEventName, moveKey, onTargetKey}
import iris.imageradc.events.PrismTargetEvent.{ImagerADCTargetEventKey, ImagerADCTargetEventName, angleKey}
import iris.imageradc.models.PrismState.MOVING
import iris.imageradc.models.{PrismPosition, PrismState}
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.concurrent.Await
import scala.concurrent.duration._

class ImagerADCTest extends ScalaTestFrameworkTestKit(EventServer) with AnyFunSuiteLike {

  import frameworkTestKit._

  override def beforeAll(): Unit = {
    super.beforeAll()
    spawnStandalone(com.typesafe.config.ConfigFactory.load("ImagerADCStandalone.conf"))
  }

  test("ADC Assembly behaviour | ESW-547") {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)
    val sequencerPrefix                         = Prefix(IRIS, "darknight")
    val connection                              = AkkaConnection(ComponentId(Prefix("IRIS.imager.adc"), ComponentType.Assembly))
    val akkaLocation                            = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get
    akkaLocation.connection shouldBe connection

    val testProbe = TestProbe[Event]()
    //Subscribe to event's which will be published by prism in it's lifecycle
    eventService.defaultSubscriber.subscribeActorRef(
      Set(
        ImagerADCStateEventKey,
        ImagerADCTargetEventKey,
        ImagerADCRetractEventKey,
        ImagerADCCurrentEventKey
      ),
      testProbe.ref
    )
    // initially prism is stopped & on target
    val currentEvent      = testProbe.expectMessageType[SystemEvent]
    val prismCurrentState = currentEvent.paramType.get(moveKey).value.values.head.name
    val isOnTarget        = currentEvent.paramType.get(onTargetKey).value.values.head
    prismCurrentState shouldBe PrismState.STOPPED.entryName
    isOnTarget shouldBe true

    val commandService = CommandServiceFactory.make(akkaLocation)
    // Retract prism from OUT to IN
    val InCommand =
      Setup(sequencerPrefix, ADCCommand.RetractSelect, None).add(PrismPosition.RetractKey.set(PrismPosition.IN.entryName))
    val response = commandService.submit(InCommand)

    val initialResponse = response.futureValue
    initialResponse shouldBe a[Started]
    // Retracting from one position to another takes 4 seconds to complete
    val finalResponse = commandService.queryFinal(initialResponse.runId)(Timeout(5.seconds))
    finalResponse.futureValue shouldBe a[Completed]

    eventually {
      val goingInEvent = testProbe.expectMessageType[SystemEvent]
      goingInEvent.paramType.get(PrismPosition.RetractKey).value.values.head.name shouldBe PrismPosition.IN.entryName
    }
    //Send Follow command to prism with target angle. This command is immediately completed.
    val FollowCommand =
      Setup(sequencerPrefix, ADCCommand.PrismFollow, None).add(ADCCommand.targetAngleKey.set(20.0))
    val followResponse = commandService.submit(FollowCommand)
    followResponse.futureValue shouldBe a[Completed]

    //verify targetAngle is set to 20.0
    eventually {
      val targetEvent = testProbe.expectMessageType[SystemEvent]
      targetEvent.eventName shouldBe ImagerADCTargetEventName
      targetEvent.paramType.get(angleKey).value.values.head shouldBe 20.0
    }

    //verify whether prism has started moving
    eventually {
      val movingEvent = testProbe.expectMessageType[SystemEvent]
      movingEvent.eventName shouldBe ImagerADCStateEventName
      movingEvent.paramType.get(moveKey).value.values.head.name shouldBe MOVING.entryName
      movingEvent.paramType.get(onTargetKey).value.values.head shouldBe false
    }

    Thread.sleep(3000)
    // After some time, current angle reaches near to targetAngle.
    eventually {
      val current = testProbe.expectMessageType[SystemEvent]
      current.eventName shouldBe ImagerADCCurrentEventName
      current.paramType.get(angleKey).value.values.head shouldBe 20.0
      current.paramType.get(angleErrorKey).value.values.head shouldBe 0.2
    }

    // Send STOP command
    val StopCommand  = Setup(sequencerPrefix, ADCCommand.PrismStop, None)
    val stopResponse = commandService.submit(StopCommand)
    stopResponse.futureValue shouldBe a[Completed]

    // expect STOPPED event
    eventually {
      val stoppedEvent = testProbe.expectMessageType[SystemEvent]
      stoppedEvent.eventName shouldBe ImagerADCStateEventName
      stoppedEvent.paramType.get(moveKey).value.values.head.name shouldBe PrismState.STOPPED.entryName
      stoppedEvent.paramType.get(onTargetKey).value.values.head shouldBe true
    }

    // Retract prism from IN to OUT
    val OutCommand =
      Setup(sequencerPrefix, ADCCommand.RetractSelect, None).add(PrismPosition.RetractKey.set(PrismPosition.OUT.entryName))
    val finalState = commandService.submit(OutCommand).futureValue
    finalState shouldBe a[Started]
    val eventualResponse = commandService.queryFinal(finalState.runId)(5.seconds).futureValue
    eventualResponse shouldBe a[Completed]

    eventually {
      val prismRetractOutState = testProbe.expectMessageType[SystemEvent]
      prismRetractOutState.eventName shouldBe ImagerADCRetractEventName
      prismRetractOutState.paramType.get(PrismPosition.RetractKey).value.values.head.name shouldBe PrismPosition.OUT.entryName
    }
  }

  test("ADC Assembly behaviour should return Invalid when concurrent (RETRACT IN) commands received | ESW-547") {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)
    val sequencerPrefix                         = Prefix(IRIS, "darknight")
    val connection                              = AkkaConnection(ComponentId(Prefix("IRIS.imager.adc"), ComponentType.Assembly))
    val akkaLocation                            = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get
    akkaLocation.connection shouldBe connection

    val testProbe = TestProbe[Event]()
    //Subscribe to event's which will be published by prism in it's lifecycle
    eventService.defaultSubscriber.subscribeActorRef(
      Set(
        ImagerADCStateEventKey,
        ImagerADCTargetEventKey,
        ImagerADCRetractEventKey,
        ImagerADCCurrentEventKey
      ),
      testProbe.ref
    )
    // initially prism is stopped & on target
    val currentEvent      = testProbe.expectMessageType[SystemEvent]
    val prismCurrentState = currentEvent.paramType.get(moveKey).value.values.head.name
    val isOnTarget        = currentEvent.paramType.get(onTargetKey).value.values.head
    prismCurrentState shouldBe PrismState.STOPPED.entryName
    isOnTarget shouldBe true

    val commandService = CommandServiceFactory.make(akkaLocation)
    // Retract prism from OUT to IN
    val InCommand =
      Setup(sequencerPrefix, ADCCommand.RetractSelect, None).add(PrismPosition.RetractKey.set(PrismPosition.IN.entryName))
    val response1 = commandService.submit(InCommand)

    // concurrent Retract prism from OUT to IN
    val response2 = commandService.submit(InCommand)

    val initialResponse = response1.futureValue
    initialResponse shouldBe a[Started]
    response2.futureValue shouldBe a[Invalid]

    // Retracting from one position to another takes 4 seconds to complete
    val finalResponse = commandService.queryFinal(initialResponse.runId)(Timeout(5.seconds))
    finalResponse.futureValue shouldBe a[Completed]
  }
}

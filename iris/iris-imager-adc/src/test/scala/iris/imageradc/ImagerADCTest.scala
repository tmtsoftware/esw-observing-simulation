package iris.imageradc

import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import org.apache.pekko.util.Timeout
import csw.command.client.CommandServiceFactory
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Invalid, Started}
import csw.params.commands.Setup
import csw.params.core.models.Angle
import csw.params.core.models.Coords.{AltAzCoord, BASE}
import csw.params.events.{Event, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS
import csw.testkit.scaladsl.CSWService.EventServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import iris.imageradc.commands.ADCCommand
import iris.imageradc.events.PrismCurrentEvent.{
  ImagerADCCurrentEventKey,
  ImagerADCCurrentEventName,
  angleErrorKey,
  currentAngleKey,
  targetAngleKey
}
import iris.imageradc.events.PrismRetractEvent.{ImagerADCRetractEventKey, ImagerADCRetractEventName}
import iris.imageradc.events.PrismStateEvent.{ImagerADCStateEventKey, ImagerADCStateEventName, followingKey, onTargetKey}
import iris.imageradc.events.TCSEvents
import iris.imageradc.models.PrismState.FOLLOWING
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

  test("ADC Assembly behaviour | ESW-547, ESW-566") {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)
    val sequencerPrefix                         = Prefix(IRIS, "darknight")
    val connection                              = PekkoConnection(ComponentId(Prefix("IRIS.imager.adc"), ComponentType.Assembly))
    val pekkoLocation                           = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get
    pekkoLocation.connection shouldBe connection

    val testProbe = TestProbe[Event]()
    // Subscribe to event's which will be published by prism in it's lifecycle
    eventService.defaultSubscriber.subscribeActorRef(
      Set(
        ImagerADCStateEventKey,
        ImagerADCRetractEventKey,
        ImagerADCCurrentEventKey
      ),
      testProbe.ref
    )
    // initially prism is stopped & on target
    val currentEvent      = testProbe.expectMessageType[SystemEvent]
    val prismCurrentState = currentEvent(followingKey).head.name
    val isOnTarget        = currentEvent(onTargetKey).head
    prismCurrentState shouldBe PrismState.STOPPED.entryName
    isOnTarget shouldBe true

    val commandService = CommandServiceFactory.make(pekkoLocation)
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
      goingInEvent(PrismPosition.RetractKey).head.name shouldBe PrismPosition.IN.entryName
    }
    // Send Follow command to prism with target angle. This command is immediately completed.
    val followCommand =
      Setup(sequencerPrefix, ADCCommand.PrismFollow, None)
//        .add(ADCCommand.targetAngleKey.set(50.0))
    val followResponse = commandService.submit(followCommand)
    followResponse.futureValue shouldBe a[Completed]
    // simulate tcs event by publishing it
    // this implicitly asserts the subscription
    import Angle._
    eventService.defaultPublisher.publish(TCSEvents.make(AltAzCoord(BASE, 40.degree, 20.degree)))

    // verify targetAngle is set to 50.0
    eventually {
      val currentEvent = testProbe.expectMessageType[SystemEvent]
      currentEvent.eventName shouldBe ImagerADCCurrentEventName
      currentEvent(targetAngleKey).head shouldBe 50.0 // we set target to  90 - alt.degree so here in test, it comes out 50, 90 - 40
    }

    // verify whether prism has started moving
    eventually {
      val movingEvent = testProbe.expectMessageType[SystemEvent]
      movingEvent.eventName shouldBe ImagerADCStateEventName
      movingEvent(followingKey).head.name shouldBe FOLLOWING.entryName
      movingEvent(onTargetKey).head shouldBe false
    }

    Thread.sleep(3000)

    // assertion to check if onTarget becomes true while following
    eventually {
      val followingEvent = testProbe.expectMessageType[SystemEvent]
      followingEvent.eventName shouldBe ImagerADCStateEventName
      followingEvent(followingKey).head.name shouldBe PrismState.FOLLOWING.entryName
      followingEvent(onTargetKey).head shouldBe true
    }

    // After some time, current angle reaches near to targetAngle.
    eventually {
      val current = testProbe.expectMessageType[SystemEvent]
      current.eventName shouldBe ImagerADCCurrentEventName
      current(currentAngleKey).head shouldBe 50.0
      current(angleErrorKey).head shouldBe 0.0
    }

    // update target to 10 degrees (90 - 80)
    eventService.defaultPublisher.publish(TCSEvents.make(AltAzCoord(BASE, 80.degree, 50.degree)))

    // assertion to check if onTarget becomes true while following
    eventually {
      val followingEvent = testProbe.expectMessageType[SystemEvent]
      followingEvent.eventName shouldBe ImagerADCStateEventName
      followingEvent(followingKey).head.name shouldBe PrismState.FOLLOWING.entryName
      followingEvent(onTargetKey).head shouldBe true
    }

    // assertion to check if prism follows the new target
    eventually {
      val current = testProbe.expectMessageType[SystemEvent]
      current.eventName shouldBe ImagerADCCurrentEventName
      current(currentAngleKey).head shouldBe 10.0
      current(angleErrorKey).head shouldBe 0.0
    }

    // Send STOP command
    val StopCommand  = Setup(sequencerPrefix, ADCCommand.PrismStop, None)
    val stopResponse = commandService.submit(StopCommand)
    stopResponse.futureValue shouldBe a[Completed]

    // expect STOPPED event
    eventually {
      val stoppedEvent = testProbe.expectMessageType[SystemEvent]
      stoppedEvent.eventName shouldBe ImagerADCStateEventName
      stoppedEvent(followingKey).head.name shouldBe PrismState.STOPPED.entryName
      stoppedEvent(onTargetKey).head shouldBe true
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
      prismRetractOutState(PrismPosition.RetractKey).head.name shouldBe PrismPosition.OUT.entryName
    }
  }

  test("ADC Assembly behaviour should return Invalid when concurrent (RETRACT IN) commands received | ESW-547") {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)
    val sequencerPrefix                         = Prefix(IRIS, "darknight")
    val connection                              = PekkoConnection(ComponentId(Prefix("IRIS.imager.adc"), ComponentType.Assembly))
    val pekkoLocation                           = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get
    pekkoLocation.connection shouldBe connection

    val testProbe = TestProbe[Event]()
    // Subscribe to event's which will be published by prism in it's lifecycle
    eventService.defaultSubscriber.subscribeActorRef(
      Set(
        ImagerADCStateEventKey,
        ImagerADCRetractEventKey,
        ImagerADCCurrentEventKey
      ),
      testProbe.ref
    )
    // initially prism is stopped & on target
    val currentEvent      = testProbe.expectMessageType[SystemEvent]
    val prismCurrentState = currentEvent(followingKey).head.name
    val isOnTarget        = currentEvent(onTargetKey).head
    prismCurrentState shouldBe PrismState.STOPPED.entryName
    isOnTarget shouldBe true

    val commandService = CommandServiceFactory.make(pekkoLocation)
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

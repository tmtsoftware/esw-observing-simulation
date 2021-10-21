package iris.imagerfilter

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.util.Timeout
import csw.command.client.CommandServiceFactory
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Invalid, Started}
import csw.params.commands.Setup
import csw.params.events.{Event, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS
import csw.testkit.scaladsl.CSWService.EventServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import iris.imagerfilter.commands.SelectCommand
import iris.imagerfilter.commands.SelectCommand.Wheel1Key
import iris.imagerfilter.events.ImagerPositionEvent
import iris.imagerfilter.events.ImagerPositionEvent.{CurrentPositionKey, DarkKey, DemandPositionKey}
import iris.imagerfilter.models.FilterWheelPosition
import iris.imagerfilter.models.FilterWheelPosition._
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.concurrent.Await
import scala.concurrent.duration._

class ImagerFilterTest extends ScalaTestFrameworkTestKit( EventServer) with AnyFunSuiteLike {

  import frameworkTestKit._

  override def beforeAll(): Unit = {
    super.beforeAll()
    // uncomment if you want one Assembly run for all tests
    spawnStandalone(com.typesafe.config.ConfigFactory.load("ImagerFilterStandalone.conf"))
  }

  test("Imager Filter Assembly behaviour | ESW-544") {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)
    val sequencerPrefix                         = Prefix(IRIS, "darknight")
    val connection                              = AkkaConnection(ComponentId(Prefix("IRIS.imager.filter"), ComponentType.Assembly))
    val akkaLocation                            = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get
    akkaLocation.connection shouldBe connection

    val testProbe = TestProbe[Event]()
    //Subscribe to event's which will be published by imager in it's lifecycle
    eventService.defaultSubscriber.subscribeActorRef(
      Set(
        ImagerPositionEvent.ImagerPositionEventKey
      ),
      testProbe.ref
    )
    // initially imager is idle & at FilterWheelPosition.Z
    val currentEvent    = testProbe.expectMessageType[SystemEvent]
    val demandPosition  = currentEvent(ImagerPositionEvent.DemandPositionKey).head.name
    val currentPosition = currentEvent(ImagerPositionEvent.CurrentPositionKey).head.name
    val dark            = currentEvent(ImagerPositionEvent.DarkKey).head

    demandPosition shouldBe FilterWheelPosition.Z.entryName
    currentPosition shouldBe FilterWheelPosition.Z.entryName
    dark shouldBe false

    val commandService = CommandServiceFactory.make(akkaLocation)

    // move position forwards
    val selectCommand =
      Setup(sequencerPrefix, SelectCommand.Name, None).add(Wheel1Key.set(H.entryName))
    val initialResponse = commandService.submit(selectCommand).futureValue

    initialResponse shouldBe a[Started]

    eventually {
      val event1 = testProbe.expectMessageType[SystemEvent]
      event1(DemandPositionKey).head.name shouldBe H.entryName
      event1(CurrentPositionKey).head.name shouldBe Y.entryName
      event1(DarkKey).head shouldBe true
    }

    eventually {
      val event2 = testProbe.expectMessageType[SystemEvent]
      event2(DemandPositionKey).head.name shouldBe H.entryName
      event2(CurrentPositionKey).head.name shouldBe J.entryName
      event2(DarkKey).head shouldBe true
    }

    eventually {
      val event3 = testProbe.expectMessageType[SystemEvent]
      event3(DemandPositionKey).head.name shouldBe H.entryName
      event3(CurrentPositionKey).head.name shouldBe H.entryName
      event3(DarkKey).head shouldBe false
    }

    val finalResponse = commandService.queryFinal(initialResponse.runId)(Timeout(2.seconds))
    finalResponse.futureValue shouldBe a[Completed]

    //Move position backwards

    val selectCommand2 =
      Setup(sequencerPrefix, SelectCommand.Name, None).add(Wheel1Key.set(Z.entryName))
    val initialResponse2 = commandService.submit(selectCommand2).futureValue

    initialResponse2 shouldBe a[Started]

    eventually {
      val event1 = testProbe.expectMessageType[SystemEvent]
      event1(DemandPositionKey).head.name shouldBe Z.entryName
      event1(CurrentPositionKey).head.name shouldBe J.entryName
      event1(DarkKey).head shouldBe true
    }

    eventually {
      val event2 = testProbe.expectMessageType[SystemEvent]
      event2(DemandPositionKey).head.name shouldBe Z.entryName
      event2(CurrentPositionKey).head.name shouldBe Y.entryName
      event2(DarkKey).head shouldBe true
    }

    eventually {
      val event3 = testProbe.expectMessageType[SystemEvent]
      event3(DemandPositionKey).head.name shouldBe Z.entryName
      event3(CurrentPositionKey).head.name shouldBe Z.entryName
      event3(DarkKey).head shouldBe false
    }

    val finalResponse2 = commandService.queryFinal(initialResponse2.runId)(Timeout(2.seconds))
    finalResponse2.futureValue shouldBe a[Completed]

  }

  test("Imager Filter Assembly behaviour should return Invalid when concurrent commands received | ESW-544") {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)
    val sequencerPrefix                         = Prefix(IRIS, "darknight")
    val connection                              = AkkaConnection(ComponentId(Prefix("IRIS.imager.filter"), ComponentType.Assembly))
    val akkaLocation                            = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get
    akkaLocation.connection shouldBe connection

    val testProbe = TestProbe[Event]()
    //Subscribe to event's which will be published by imager in it's lifecycle
    eventService.defaultSubscriber.subscribeActorRef(
      Set(
        ImagerPositionEvent.ImagerPositionEventKey
      ),
      testProbe.ref
    )
    // initially imager is idle & at FilterWheelPosition.Z
    val currentEvent    = testProbe.expectMessageType[SystemEvent]
    val demandPosition  = currentEvent(ImagerPositionEvent.DemandPositionKey).head.name
    val currentPosition = currentEvent(ImagerPositionEvent.CurrentPositionKey).head.name
    val dark            = currentEvent(ImagerPositionEvent.DarkKey).head

    demandPosition shouldBe FilterWheelPosition.Z.entryName
    currentPosition shouldBe FilterWheelPosition.Z.entryName
    dark shouldBe false

    val commandService = CommandServiceFactory.make(akkaLocation)

    // move position forwards
    val selectCommand =
      Setup(sequencerPrefix, SelectCommand.Name, None).add(Wheel1Key.set(H.entryName))
    val initialResponse = commandService.submit(selectCommand).futureValue

    //concurrent move
    val command2Response = commandService.submit(selectCommand).futureValue

    initialResponse shouldBe a[Started]
    command2Response shouldBe a[Invalid]
  }
}

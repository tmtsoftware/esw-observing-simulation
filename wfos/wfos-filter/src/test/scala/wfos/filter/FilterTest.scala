package wfos.filter

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.util.Timeout
import csw.command.client.CommandServiceFactory
import csw.location.api.models.Connection.AkkaConnection
import csw.prefix.models.Prefix
import csw.location.api.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Invalid, Started}
import csw.params.commands.Setup
import csw.params.events.{Event, SystemEvent}
import csw.prefix.models.Subsystem.WFOS
import csw.testkit.scaladsl.CSWService.{AlarmServer, EventServer}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import wfos.filter.commands.SelectCommand.Wheel1Key
import wfos.filter.models.FilterWheelPosition._
import org.scalatest.funsuite.AnyFunSuiteLike
import wfos.filter.commands.SelectCommand
import wfos.filter.events.FilterPositionEvent
import wfos.filter.models.FilterWheelPosition

import scala.concurrent.Await
import scala.concurrent.duration._

class FilterTest extends ScalaTestFrameworkTestKit(AlarmServer, EventServer) with AnyFunSuiteLike {

  import frameworkTestKit._
  val filterPrefix: Prefix = Prefix(WFOS, "red.filter")
  val filterPositionEvent  = new FilterPositionEvent(filterPrefix)

  override def beforeAll(): Unit = {
    super.beforeAll()
    // uncomment if you want one Assembly run for all tests
    spawnStandalone(com.typesafe.config.ConfigFactory.load("RedFilterStandalone.conf"))
  }

  test("Filter Assembly behaviour | ESW-556, ESW-557") {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)
    val sequencerPrefix                         = Prefix(WFOS, "darknight")
    val connection                              = AkkaConnection(ComponentId(filterPrefix, ComponentType.Assembly))
    val akkaLocation                            = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get
    akkaLocation.connection shouldBe connection

    val testProbe = TestProbe[Event]()
    //Subscribe to event's which will be published by red filter in it's lifecycle
    eventService.defaultSubscriber.subscribeActorRef(
      Set(
        filterPositionEvent.FilterPositionEventKey
      ),
      testProbe.ref
    )
    // initially red filter is idle & at FilterWheelPosition.Z
    val currentEvent    = testProbe.expectMessageType[SystemEvent]
    val demandPosition  = currentEvent.paramType.get(filterPositionEvent.DemandPositionKey).value.values.head.name
    val currentPosition = currentEvent.paramType.get(filterPositionEvent.CurrentPositionKey).value.values.head.name
    val dark            = currentEvent.paramType.get(filterPositionEvent.DarkKey).value.values.head

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
      event1.paramType.get(filterPositionEvent.DemandPositionKey).value.values.head.name shouldBe H.entryName
      event1.paramType.get(filterPositionEvent.CurrentPositionKey).value.values.head.name shouldBe Y.entryName
      event1.paramType.get(filterPositionEvent.DarkKey).value.values.head shouldBe true
    }

    eventually {
      val event2 = testProbe.expectMessageType[SystemEvent]
      event2.paramType.get(filterPositionEvent.DemandPositionKey).value.values.head.name shouldBe H.entryName
      event2.paramType.get(filterPositionEvent.CurrentPositionKey).value.values.head.name shouldBe J.entryName
      event2.paramType.get(filterPositionEvent.DarkKey).value.values.head shouldBe true
    }

    eventually {
      val event3 = testProbe.expectMessageType[SystemEvent]
      event3.paramType.get(filterPositionEvent.DemandPositionKey).value.values.head.name shouldBe H.entryName
      event3.paramType.get(filterPositionEvent.CurrentPositionKey).value.values.head.name shouldBe H.entryName
      event3.paramType.get(filterPositionEvent.DarkKey).value.values.head shouldBe false
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
      event1.paramType.get(filterPositionEvent.DemandPositionKey).value.values.head.name shouldBe Z.entryName
      event1.paramType.get(filterPositionEvent.CurrentPositionKey).value.values.head.name shouldBe J.entryName
      event1.paramType.get(filterPositionEvent.DarkKey).value.values.head shouldBe true
    }

    eventually {
      val event2 = testProbe.expectMessageType[SystemEvent]
      event2.paramType.get(filterPositionEvent.DemandPositionKey).value.values.head.name shouldBe Z.entryName
      event2.paramType.get(filterPositionEvent.CurrentPositionKey).value.values.head.name shouldBe Y.entryName
      event2.paramType.get(filterPositionEvent.DarkKey).value.values.head shouldBe true
    }

    eventually {
      val event3 = testProbe.expectMessageType[SystemEvent]
      event3.paramType.get(filterPositionEvent.DemandPositionKey).value.values.head.name shouldBe Z.entryName
      event3.paramType.get(filterPositionEvent.CurrentPositionKey).value.values.head.name shouldBe Z.entryName
      event3.paramType.get(filterPositionEvent.DarkKey).value.values.head shouldBe false
    }

    val finalResponse2 = commandService.queryFinal(initialResponse2.runId)(Timeout(2.seconds))
    finalResponse2.futureValue shouldBe a[Completed]

  }

  test("Filter Assembly behaviour should return Invalid when concurrent commands received | ESW-556, ESW-557") {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)
    val sequencerPrefix                         = Prefix(WFOS, "darknight")
    val connection                              = AkkaConnection(ComponentId(Prefix("WFOS.red.filter"), ComponentType.Assembly))
    val akkaLocation                            = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get
    akkaLocation.connection shouldBe connection

    val testProbe = TestProbe[Event]()
    //Subscribe to event's which will be published by red filter in it's lifecycle
    eventService.defaultSubscriber.subscribeActorRef(
      Set(
        filterPositionEvent.FilterPositionEventKey
      ),
      testProbe.ref
    )
    // initially red filter is idle & at FilterWheelPosition.Z
    val currentEvent    = testProbe.expectMessageType[SystemEvent]
    val demandPosition  = currentEvent.paramType.get(filterPositionEvent.DemandPositionKey).value.values.head.name
    val currentPosition = currentEvent.paramType.get(filterPositionEvent.CurrentPositionKey).value.values.head.name
    val dark            = currentEvent.paramType.get(filterPositionEvent.DarkKey).value.values.head

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

package wfos.filter

import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import org.apache.pekko.util.Timeout
import csw.command.client.CommandServiceFactory
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Invalid, Started}
import csw.params.commands.Setup
import csw.params.events.{Event, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.WFOS
import csw.testkit.scaladsl.CSWService.EventServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.funsuite.AnyFunSuiteLike
import wfos.filter.commands.BlueSelectCommand
import wfos.filter.commands.BlueSelectCommand.Wheel1Key
import wfos.filter.events.BlueFilterPositionEvent
import wfos.filter.models.BlueFilterWheelPosition
import wfos.filter.models.BlueFilterWheelPosition._

import scala.concurrent.Await
import scala.concurrent.duration._

class BlueFilterTest extends ScalaTestFrameworkTestKit(EventServer) with AnyFunSuiteLike {

  import frameworkTestKit._
  val prefix: Prefix      = Prefix(WFOS, "blue.filter")
  val filterPositionEvent = new BlueFilterPositionEvent(prefix)

  override def beforeAll(): Unit = {
    super.beforeAll()
    // uncomment if you want one Assembly run for all tests
    spawnStandalone(com.typesafe.config.ConfigFactory.load("BlueFilterStandalone.conf"))
  }
  test(s"$prefix - Assembly behaviour | ESW-557") {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)
    val sequencerPrefix                         = Prefix(WFOS, "darknight")
    val connection                              = PekkoConnection(ComponentId(prefix, ComponentType.Assembly))

    val pekkoLocation = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get
    pekkoLocation.connection shouldBe connection

    val testProbe = TestProbe[Event]()
    // Subscribe to event's which will be published by blue filter in it's lifecycle
    eventService.defaultSubscriber.subscribeActorRef(
      Set(
        filterPositionEvent.FilterPositionEventKey
      ),
      testProbe.ref
    )
    // initially filter is idle & at initial position
    val currentEvent    = testProbe.expectMessageType[SystemEvent]
    val demandPosition  = currentEvent(filterPositionEvent.DemandPositionKey).head.name
    val currentPosition = currentEvent(filterPositionEvent.CurrentPositionKey).head.name
    val dark            = currentEvent(filterPositionEvent.DarkKey).head

    demandPosition shouldBe BlueFilterWheelPosition.UPrime.entryName
    currentPosition shouldBe BlueFilterWheelPosition.UPrime.entryName
    dark shouldBe false

    val commandService = CommandServiceFactory.make(pekkoLocation)

    // move position forwards
    val setup =
      Setup(sequencerPrefix, BlueSelectCommand.Name, None).add(
        BlueSelectCommand.Wheel1Key.set(BlueFilterWheelPosition.FusedSilica.entryName)
      )
    val initialResponse = commandService.submit(setup).futureValue

    initialResponse shouldBe a[Started]

    eventually {
      val event1 = testProbe.expectMessageType[SystemEvent]
      event1(filterPositionEvent.DemandPositionKey).head.name shouldBe FusedSilica.entryName
      event1(filterPositionEvent.CurrentPositionKey).head.name shouldBe GPrime.entryName
      event1(filterPositionEvent.DarkKey).head shouldBe true
    }

    eventually {
      val event3 = testProbe.expectMessageType[SystemEvent]
      event3(filterPositionEvent.DemandPositionKey).head.name shouldBe FusedSilica.entryName
      event3(filterPositionEvent.CurrentPositionKey).head.name shouldBe FusedSilica.entryName
      event3(filterPositionEvent.DarkKey).head shouldBe false
    }

    val finalResponse = commandService.queryFinal(initialResponse.runId)(Timeout(2.seconds))
    finalResponse.futureValue shouldBe a[Completed]

    // Move position backwards

    val selectCommand2 =
      Setup(sequencerPrefix, BlueSelectCommand.Name, None).add(Wheel1Key.set(UPrime.entryName))
    val initialResponse2 = commandService.submit(selectCommand2).futureValue

    initialResponse2 shouldBe a[Started]

    eventually {
      val event1 = testProbe.expectMessageType[SystemEvent]
      event1(filterPositionEvent.DemandPositionKey).head.name shouldBe UPrime.entryName
      event1(filterPositionEvent.CurrentPositionKey).head.name shouldBe GPrime.entryName
      event1(filterPositionEvent.DarkKey).head shouldBe true
    }

    eventually {
      val event3 = testProbe.expectMessageType[SystemEvent]
      event3(filterPositionEvent.DemandPositionKey).head.name shouldBe UPrime.entryName
      event3(filterPositionEvent.CurrentPositionKey).head.name shouldBe UPrime.entryName
      event3(filterPositionEvent.DarkKey).head shouldBe false
    }

    val finalResponse2 = commandService.queryFinal(initialResponse2.runId)(Timeout(2.seconds))
    finalResponse2.futureValue shouldBe a[Completed]
  }

  test(s"${prefix} - Assembly behaviour should return Invalid when concurrent commands received | ESW-557") {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)
    val sequencerPrefix                         = Prefix(WFOS, "darknight")
    val connection                              = PekkoConnection(ComponentId(prefix, ComponentType.Assembly))
    val pekkoLocation                           = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get
    pekkoLocation.connection shouldBe connection

    val testProbe = TestProbe[Event]()
    // Subscribe to event's which will be published by blue filter in it's lifecycle
    eventService.defaultSubscriber.subscribeActorRef(
      Set(
        filterPositionEvent.FilterPositionEventKey
      ),
      testProbe.ref
    )
    // initially filter is idle & at initial position
    val currentEvent    = testProbe.expectMessageType[SystemEvent]
    val demandPosition  = currentEvent(filterPositionEvent.DemandPositionKey).head.name
    val currentPosition = currentEvent(filterPositionEvent.CurrentPositionKey).head.name
    val dark            = currentEvent(filterPositionEvent.DarkKey).head

    demandPosition shouldBe BlueFilterWheelPosition.UPrime.entryName
    currentPosition shouldBe BlueFilterWheelPosition.UPrime.entryName
    dark shouldBe false

    val commandService = CommandServiceFactory.make(pekkoLocation)

    // move position forwards
    val selectCommand =
      Setup(sequencerPrefix, BlueSelectCommand.Name, None).add(Wheel1Key.set(FusedSilica.entryName))
    val initialResponse = commandService.submit(selectCommand).futureValue

    // concurrent move
    val command2Response = commandService.submit(selectCommand).futureValue

    initialResponse shouldBe a[Started]
    command2Response shouldBe a[Invalid]
  }

}

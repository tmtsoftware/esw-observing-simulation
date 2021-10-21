package iris.ifsres

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.util.Timeout
import csw.command.client.CommandServiceFactory
import csw.location.api.models.Connection.AkkaConnection
import csw.prefix.models.Prefix
import csw.location.api.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Invalid, Started}
import csw.params.commands.Setup
import csw.params.events.{Event, SystemEvent}
import csw.prefix.models.Subsystem.IRIS
import csw.testkit.scaladsl.CSWService.{AlarmServer, EventServer}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import iris.ifsres.commands.SelectCommand
import iris.ifsres.commands.SelectCommand.SpectralResolutionKey
import iris.ifsres.events.IfsPositionEvent._
import iris.ifsres.models.ResWheelPosition._
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.concurrent.Await
import scala.concurrent.duration._

class IfsResTest extends ScalaTestFrameworkTestKit(AlarmServer, EventServer) with AnyFunSuiteLike {

  import frameworkTestKit._

  override def beforeAll(): Unit = {
    super.beforeAll()
    // uncomment if you want one Assembly run for all tests
    spawnStandalone(com.typesafe.config.ConfigFactory.load("IfsResStandalone.conf"))
  }

  test("IFS Resolution Assembly behaviour | ESW-545") {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)
    val sequencerPrefix                         = Prefix(IRIS, "darknight")
    val connection                              = AkkaConnection(ComponentId(Prefix("IRIS.ifs.res"), ComponentType.Assembly))
    val akkaLocation                            = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get
    akkaLocation.connection shouldBe connection

    val testProbe = TestProbe[Event]()
    //Subscribe to event's which will be published by imager in it's lifecycle
    eventService.defaultSubscriber.subscribeActorRef(
      Set(
        IfsResPositionEventKey
      ),
      testProbe.ref
    )
    // initially res is idle & at R4000_Z
    val currentEvent    = testProbe.expectMessageType[SystemEvent]
    val demandPosition  = currentEvent(TargetPositionKey).head.name
    val currentPosition = currentEvent(CurrentPositionKey).head.name

    demandPosition shouldBe R4000_Z.entryName
    currentPosition shouldBe R4000_Z.entryName

    val commandService = CommandServiceFactory.make(akkaLocation)

    // move position forwards
    val selectCommand =
      Setup(sequencerPrefix, SelectCommand.Name, None).add(SpectralResolutionKey.set(R4000_H.entryName))
    val initialResponse = commandService.submit(selectCommand).futureValue

    initialResponse shouldBe a[Started]

    eventually {
      val event1 = testProbe.expectMessageType[SystemEvent]
      event1(TargetPositionKey).head.name shouldBe R4000_H.entryName
      event1(CurrentPositionKey).head.name shouldBe R4000_Y.entryName
    }

    eventually {
      val event2 = testProbe.expectMessageType[SystemEvent]
      event2(TargetPositionKey).head.name shouldBe R4000_H.entryName
      event2(CurrentPositionKey).head.name shouldBe R4000_J.entryName
    }

    eventually {
      val event3 = testProbe.expectMessageType[SystemEvent]
      event3(TargetPositionKey).head.name shouldBe R4000_H.entryName
      event3(CurrentPositionKey).head.name shouldBe R4000_H.entryName
    }

    val finalResponse = commandService.queryFinal(initialResponse.runId)(Timeout(2.seconds))
    finalResponse.futureValue shouldBe a[Completed]

    //Move position backwards

    val selectCommand2 =
      Setup(sequencerPrefix, SelectCommand.Name, None).add(SpectralResolutionKey.set(R4000_Z.entryName))
    val initialResponse2 = commandService.submit(selectCommand2).futureValue

    initialResponse2 shouldBe a[Started]

    eventually {
      val event1 = testProbe.expectMessageType[SystemEvent]
      event1(TargetPositionKey).head.name shouldBe R4000_Z.entryName
      event1(CurrentPositionKey).head.name shouldBe R4000_J.entryName
    }

    eventually {
      val event2 = testProbe.expectMessageType[SystemEvent]
      event2(TargetPositionKey).head.name shouldBe R4000_Z.entryName
      event2(CurrentPositionKey).head.name shouldBe R4000_Y.entryName
    }

    eventually {
      val event3 = testProbe.expectMessageType[SystemEvent]
      event3(TargetPositionKey).head.name shouldBe R4000_Z.entryName
      event3(CurrentPositionKey).head.name shouldBe R4000_Z.entryName
    }

    val finalResponse2 = commandService.queryFinal(initialResponse2.runId)(Timeout(2.seconds))
    finalResponse2.futureValue shouldBe a[Completed]

  }

  test("IFS Resolution Assembly behaviour should return Invalid when concurrent commands received | ESW-545") {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)
    val sequencerPrefix                         = Prefix(IRIS, "darknight")
    val connection                              = AkkaConnection(ComponentId(Prefix("IRIS.ifs.res"), ComponentType.Assembly))
    val akkaLocation                            = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get
    akkaLocation.connection shouldBe connection

    val testProbe = TestProbe[Event]()
    //Subscribe to event's which will be published by imager in it's lifecycle
    eventService.defaultSubscriber.subscribeActorRef(
      Set(
        IfsResPositionEventKey
      ),
      testProbe.ref
    )
    // initially imager is idle & at FilterWheelPosition.Z
    val currentEvent    = testProbe.expectMessageType[SystemEvent]
    val demandPosition  = currentEvent(TargetPositionKey).head.name
    val currentPosition = currentEvent(CurrentPositionKey).head.name

    demandPosition shouldBe R4000_Z.entryName
    currentPosition shouldBe R4000_Z.entryName

    val commandService = CommandServiceFactory.make(akkaLocation)

    // move position forwards
    val selectCommand =
      Setup(sequencerPrefix, SelectCommand.Name, None).add(SpectralResolutionKey.set(R4000_H.entryName))
    val initialResponse = commandService.submit(selectCommand).futureValue

    //concurrent move
    val command2Response = commandService.submit(selectCommand).futureValue

    initialResponse shouldBe a[Started]
    command2Response shouldBe a[Invalid]
  }
}

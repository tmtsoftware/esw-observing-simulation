package iris.ifsscale

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
import iris.ifsscale.commands.SelectCommand
import iris.ifsscale.commands.SelectCommand.ScaleKey
import iris.ifsscale.events.IfsScaleEvent._
import iris.ifsscale.models.ScaleLevel._
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.concurrent.Await
import scala.concurrent.duration._

class IfsScaleTest extends ScalaTestFrameworkTestKit(EventServer) with AnyFunSuiteLike {

  import frameworkTestKit._

  override def beforeAll(): Unit = {
    super.beforeAll()
    // uncomment if you want one Assembly run for all tests
    spawnStandalone(com.typesafe.config.ConfigFactory.load("IfsScaleStandalone.conf"))
  }

  test("IFS Scale Assembly behaviour | ESW-546") {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)
    val sequencerPrefix                         = Prefix(IRIS, "darknight")
    val connection                              = AkkaConnection(ComponentId(Prefix("IRIS.ifs.scale"), ComponentType.Assembly))
    val akkaLocation                            = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get
    akkaLocation.connection shouldBe connection

    val testProbe = TestProbe[Event]()
    //Subscribe to event's which will be published by scale assembly in it's lifecycle
    eventService.defaultSubscriber.subscribeActorRef(
      Set(
        IfsScaleEventKey
      ),
      testProbe.ref
    )
    // initially res is idle & at S25
    val currentEvent    = testProbe.expectMessageType[SystemEvent]
    val demandPosition  = currentEvent(TargetScaleKey).head.name
    val currentPosition = currentEvent(CurrentScaleKey).head.name

    demandPosition shouldBe S25.entryName
    currentPosition shouldBe S25.entryName

    val commandService = CommandServiceFactory.make(akkaLocation)

    // change scale
    val selectCommand =
      Setup(sequencerPrefix, SelectCommand.Name, None).add(ScaleKey.set(S4.entryName))
    val initialResponse = commandService.submit(selectCommand).futureValue

    initialResponse shouldBe a[Started]

    eventually {
      val event1 = testProbe.expectMessageType[SystemEvent]
      event1(TargetScaleKey).head.name shouldBe S4.entryName
      event1(CurrentScaleKey).head.name shouldBe S4.entryName
    }

    val finalResponse = commandService.queryFinal(initialResponse.runId)(Timeout(2.seconds))
    finalResponse.futureValue shouldBe a[Completed]
  }

  test("IFS Scale Assembly behaviour should return Invalid when concurrent commands received | ESW-546") {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)
    val sequencerPrefix                         = Prefix(IRIS, "darknight")
    val connection                              = AkkaConnection(ComponentId(Prefix("IRIS.ifs.scale"), ComponentType.Assembly))
    val akkaLocation                            = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get
    akkaLocation.connection shouldBe connection

    val testProbe = TestProbe[Event]()
    //Subscribe to event's which will be published by scale assembly in it's lifecycle
    eventService.defaultSubscriber.subscribeActorRef(
      Set(
        IfsScaleEventKey
      ),
      testProbe.ref
    )
    val commandService = CommandServiceFactory.make(akkaLocation)

    // change scale
    val selectCommand =
      Setup(sequencerPrefix, SelectCommand.Name, None).add(ScaleKey.set(S9.entryName))
    val initialResponse = commandService.submit(selectCommand).futureValue

    //concurrent change scale
    val command2Response = commandService.submit(selectCommand).futureValue

    initialResponse shouldBe a[Started]
    command2Response shouldBe a[Invalid]
  }
}

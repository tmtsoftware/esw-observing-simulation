package iris.imager.detector

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.remote.transport.ActorTransportAdapter.AskTimeout
import csw.command.client.CommandServiceFactory
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Started}
import csw.params.commands.{Observe, Setup}
import csw.params.core.models.{ExposureId, ObsId}
import csw.params.events.{Event, EventKey, IRDetectorEvent, ObserveEvent, ObserveEventKeys, ObserveEventNames}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS
import csw.testkit.scaladsl.CSWService.EventServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ControllerActorTest extends ScalaTestFrameworkTestKit(EventServer) with AnyFunSuiteLike {
  import frameworkTestKit._

  override def beforeAll(): Unit = {
    super.beforeAll()
    spawnStandalone(com.typesafe.config.ConfigFactory.load("ImagerDetectorStandalone.conf"))
  }
  test("Imager detector test | ESW-552") {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)
    val testPrefix                              = Prefix(IRIS, "darknight")
    val detectorPrefix                          = Prefix("IRIS.imager.detector")
    val connection                              = AkkaConnection(ComponentId(detectorPrefix, ComponentType.Assembly))
    val akkaLocation                            = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get
    akkaLocation.connection shouldBe connection

    val testProbe = TestProbe[Event]()
    //Subscribe to event's which will be published by prism in it's lifecycle
    eventService.defaultSubscriber.subscribeActorRef(
      Set(
        EventKey(detectorPrefix, ObserveEventNames.ExposureStart),
        EventKey(detectorPrefix, ObserveEventNames.ExposureEnd),
        EventKey(detectorPrefix, ObserveEventNames.ExposureAborted),
        EventKey(detectorPrefix, ObserveEventNames.DataWriteEnd),
        EventKey(detectorPrefix, ObserveEventNames.DataWriteStart)
      ),
      testProbe.ref
    )
    Thread.sleep(500)
    val commandService = CommandServiceFactory.make(akkaLocation)

    val eventualResponse = commandService.submit(Setup(testPrefix, Constants.Initialize)).futureValue
    eventualResponse shouldBe a[Started]

    val response = commandService.queryFinal(eventualResponse.runId).futureValue
    response shouldBe a[Completed]

    val obsId = ObsId("2020A-001-123")
    val filename = "imagerFile1"

    val configure = Setup(
      testPrefix,
      Constants.LoadConfiguration,
      Some(obsId),
      Set(
        ObserveEventKeys.exposureId.set("2020A-001-123-IRIS-IMG-DRK1-0023"),
        ObserveEventKeys.filename.set(filename),
        Constants.rampsKey.set(5),
        Constants.rampIntegrationTimeKey.set(5)
      )
    )
    val configureResponse = commandService.submit(configure).futureValue
    val finalConfigureRes = commandService.queryFinal(configureResponse.runId).futureValue
    finalConfigureRes shouldBe a[Completed]

    val started = commandService.submit(Observe(testPrefix, Constants.StartExposure, Some(obsId))).futureValue
    started shouldBe a[Started]
    val exposureId = ExposureId("2020A-001-123-IRIS-IMG-DRK1-0023")

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name === ObserveEventNames.ExposureStart.name
      ExposureId(event.get(ObserveEventKeys.exposureId).get.head) shouldBe exposureId
    }
    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name === ObserveEventNames.ExposureEnd.name
      ExposureId(event.get(ObserveEventKeys.exposureId).get.head) shouldBe exposureId
    }
    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name === ObserveEventNames.DataWriteStart.name
      event.get(ObserveEventKeys.filename).get.head shouldBe filename
      ExposureId(event.get(ObserveEventKeys.exposureId).get.head) shouldBe exposureId
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name === ObserveEventNames.DataWriteEnd.name
      event.get(ObserveEventKeys.filename).get.head shouldBe filename
      ExposureId(event.get(ObserveEventKeys.exposureId).get.head) shouldBe exposureId
    }

  }
}

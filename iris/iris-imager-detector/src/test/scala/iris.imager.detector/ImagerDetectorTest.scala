package iris.imager.detector

import akka.Done
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Invalid, Started}
import csw.params.commands.{Observe, Setup}
import csw.params.core.models.{ExposureId, ObsId}
import csw.params.events._
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS
import csw.testkit.scaladsl.CSWService.EventServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ImagerDetectorTest extends ScalaTestFrameworkTestKit(EventServer) with AnyFunSuiteLike {
  import frameworkTestKit._

  private val seconds                                  = 10.seconds
  private implicit val timeout: Timeout                = Timeout(seconds)
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(seconds)
  private val obsId                                    = ObsId("2020A-001-123")
  private val exposureId                               = ExposureId("2020A-001-123-IRIS-IMG-DRK1-0023")
  private val filename                                 = "imagerFile1"
  private val testPrefix                               = Prefix(IRIS, "darknight")
  private val detectorPrefix                           = Prefix("IRIS.imager.detector")
  private val connection                               = AkkaConnection(ComponentId(detectorPrefix, ComponentType.Assembly))

  override def beforeAll(): Unit = {
    super.beforeAll()
    spawnStandalone(com.typesafe.config.ConfigFactory.load("ImagerDetectorStandalone.conf"))
  }

  test("test the whole cycle of an observation | ESW-552") {
    val akkaLocation = locationService.resolve(connection, seconds).futureValue.get
    akkaLocation.connection shouldBe connection

    val testProbe = TestProbe[Event]()

    //Subscribe to event's which will be published by detector in it's lifecycle
    val subscription = eventService.defaultSubscriber.subscribeActorRef(
      Set(
        EventKey(detectorPrefix, ObserveEventNames.ExposureStart),
        EventKey(detectorPrefix, ObserveEventNames.ExposureEnd),
        EventKey(detectorPrefix, ObserveEventNames.ExposureAborted),
        EventKey(detectorPrefix, ObserveEventNames.DataWriteEnd),
        EventKey(detectorPrefix, ObserveEventNames.DataWriteStart)
      ),
      testProbe.ref
    )

    subscription.ready().futureValue shouldBe Done

    val commandService: CommandService = assertAssemblyIsConfigured(testPrefix, akkaLocation)

    val exposureStarted = commandService.submit(Observe(testPrefix, Constants.StartExposure, Some(obsId))).futureValue
    exposureStarted shouldBe a[Started]

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name === ObserveEventNames.ExposureStart.name
      ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
    }
    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name === ObserveEventNames.ExposureEnd.name
      ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
    }
    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name === ObserveEventNames.DataWriteStart.name
      event(ObserveEventKeys.filename).head shouldBe filename
      ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
    }

    eventually {
      val event = testProbe.expectMessageType[ObserveEvent]
      event.eventName.name === ObserveEventNames.DataWriteEnd.name
      event(ObserveEventKeys.filename).head shouldBe filename
      ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
    }

    val exposureFinished = commandService.queryFinal(exposureStarted.runId).futureValue
    exposureFinished shouldBe a[Completed]

    val shutdown = commandService.submit(Setup(testPrefix, Constants.Shutdown)).futureValue
    shutdown shouldBe a[Started]

    commandService.queryFinal(shutdown.runId).futureValue shouldBe a[Completed]
  }

  test("Detector Assembly behaviour should return Invalid when concurrent (Start Exposure) commands received | ESW-552") {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)
    val testPrefix                              = Prefix(IRIS, "darknight")
    val detectorPrefix                          = Prefix("IRIS.imager.detector")
    val connection                              = AkkaConnection(ComponentId(detectorPrefix, ComponentType.Assembly))
    val akkaLocation                            = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get
    akkaLocation.connection shouldBe connection

    val testProbe = TestProbe[Event]()
    //Subscribe to event's which will be published by prism in it's lifecycle
    val subscription = eventService.defaultSubscriber.subscribeActorRef(
      Set(
        EventKey(detectorPrefix, ObserveEventNames.ExposureStart),
        EventKey(detectorPrefix, ObserveEventNames.ExposureEnd),
        EventKey(detectorPrefix, ObserveEventNames.ExposureAborted),
        EventKey(detectorPrefix, ObserveEventNames.DataWriteEnd),
        EventKey(detectorPrefix, ObserveEventNames.DataWriteStart)
      ),
      testProbe.ref
    )

    subscription.ready().futureValue shouldBe Done

    val commandService: CommandService = assertAssemblyIsConfigured(testPrefix, akkaLocation)

    val startExposure = Observe(testPrefix, Constants.StartExposure, Some(obsId))

    val exposureStarted = commandService.submit(startExposure).futureValue
    val exposureStarted2 = commandService.submit(startExposure).futureValue
    exposureStarted shouldBe a[Started]
    exposureStarted2 shouldBe a[Invalid]

  }

  private def assertAssemblyIsConfigured(testPrefix: Prefix, akkaLocation: AkkaLocation) = {
    val commandService = CommandServiceFactory.make(akkaLocation)

    val eventualResponse = commandService.submit(Setup(testPrefix, Constants.Initialize)).futureValue
    eventualResponse shouldBe a[Started]

    val response = commandService.queryFinal(eventualResponse.runId).futureValue
    response shouldBe a[Completed]

    val configure = Setup(
      testPrefix,
      Constants.LoadConfiguration,
      Some(obsId),
      Set(
        ObserveEventKeys.exposureId.set(exposureId.toString),
        ObserveEventKeys.filename.set(filename),
        Constants.rampsKey.set(5),
        Constants.rampIntegrationTimeKey.set(5)
      )
    )
    val configureStarted  = commandService.submit(configure).futureValue
    val finalConfigureRes = commandService.queryFinal(configureStarted.runId).futureValue
    finalConfigureRes shouldBe a[Completed]
    commandService
  }
}

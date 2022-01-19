package iris.detector

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

class DetectorTest extends ScalaTestFrameworkTestKit(EventServer) with AnyFunSuiteLike {
  import frameworkTestKit._
  private val imagerDetectorPrefix                     = Prefix("IRIS.imager.detector")
  private val ifsDetectorPrefix                        = Prefix("IRIS.ifs.detector")
  val detectors                                        = List(imagerDetectorPrefix, ifsDetectorPrefix)
  private val seconds                                  = 10.seconds
  private implicit val timeout: Timeout                = Timeout(seconds)
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(seconds)
  private val filename                                 = "/tmp/imagerFile1.fits"
  private val testPrefix                               = Prefix(IRIS, "darknight")
  var obsIdCount                                       = 0

  override def beforeAll(): Unit = {
    super.beforeAll()
    spawnStandalone(com.typesafe.config.ConfigFactory.load("ImagerDetectorStandalone.conf"))
    spawnStandalone(com.typesafe.config.ConfigFactory.load("IFSDetectorStandalone.conf"))
  }

  detectors.foreach { detectorPrefix =>
    {
      test(s" $detectorPrefix - test the whole cycle of an observation | ESW-552, ESW-553") {
        obsIdCount += 1
        val obsId        = ObsId(s"2020A-001-12$obsIdCount")
        val exposureId   = ExposureId(s"$obsId-IRIS-IMG-DRK1-0023")
        val connection   = AkkaConnection(ComponentId(detectorPrefix, ComponentType.Assembly))
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

        val detectorDataTestProbe = TestProbe[Event]()
        val dataSubscription = eventService.defaultSubscriber.subscribeActorRef(
          Set(EventKey(detectorPrefix, ObserveEventNames.IRDetectorExposureData)),
          detectorDataTestProbe.ref
        )
        dataSubscription.ready().futureValue shouldBe Done

        val commandService: CommandService = assertAssemblyIsConfigured(obsId, exposureId, testPrefix, akkaLocation)

        val exposureStarted = commandService.submit(Observe(testPrefix, Constants.StartExposure, Some(obsId))).futureValue
        exposureStarted shouldBe a[Started]

        eventually {
          val event = testProbe.expectMessageType[ObserveEvent]
          event.eventName.name shouldBe ObserveEventNames.ExposureStart.name
          ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
        }
        eventually {
          val event = testProbe.expectMessageType[ObserveEvent]
          event.eventName.name shouldBe ObserveEventNames.ExposureEnd.name
          ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
        }
        eventually {
          val event = testProbe.expectMessageType[ObserveEvent]
          event.eventName.name shouldBe ObserveEventNames.DataWriteStart.name
          event(ObserveEventKeys.filename).head shouldBe filename
          ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
        }

        eventually {
          val event = testProbe.expectMessageType[ObserveEvent]
          event.eventName.name shouldBe ObserveEventNames.DataWriteEnd.name
          event(ObserveEventKeys.filename).head shouldBe filename
          ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
        }

        val exposureFinished = commandService.queryFinal(exposureStarted.runId).futureValue
        exposureFinished shouldBe a[Completed]

        val dataMessages                      = detectorDataTestProbe.receiveMessages(7)
        val expectedRemainingIntegrationTimes = List(8800, 7040, 5280, 3520, 1760, 0)

        (0 to 5).foreach { ii =>
          // skip first message, which is an InvalidEvent published on subscription
          dataMessages(ii + 1) match {
            case _: SystemEvent => fail("Should not receive SystemEvent")
            case event: ObserveEvent =>
              event.eventName shouldBe ObserveEventNames.IRDetectorExposureData
              ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
              event(ObserveEventKeys.rampsComplete).head shouldBe ii
              event(ObserveEventKeys.remainingExposureTime).head shouldBe expectedRemainingIntegrationTimes(ii)
          }
        }
        val shutdown = commandService.submit(Setup(testPrefix, Constants.Shutdown)).futureValue
        shutdown shouldBe a[Started]

        commandService.queryFinal(shutdown.runId).futureValue shouldBe a[Completed]
      }

      test(s" $detectorPrefix - test abort exposure within an observation | ESW-552, ESW-553") {
        obsIdCount += 1
        val obsId        = ObsId(s"2020A-001-12$obsIdCount")
        val exposureId   = ExposureId(s"$obsId-IRIS-IMG-DRK1-0023")
        val connection   = AkkaConnection(ComponentId(detectorPrefix, ComponentType.Assembly))
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

        val commandService: CommandService = assertAssemblyIsConfigured(obsId, exposureId, testPrefix, akkaLocation)

        val exposureStarted = commandService.submit(Observe(testPrefix, Constants.StartExposure, Some(obsId))).futureValue
        exposureStarted shouldBe a[Started]

        eventually {
          val event = testProbe.expectMessageType[ObserveEvent]
          event.eventName.name shouldBe ObserveEventNames.ExposureStart.name
          ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
        }

        val exposureAborted = commandService.submit(Observe(testPrefix, Constants.AbortExposure, Some(obsId))).futureValue

        eventually {
          val event = testProbe.expectMessageType[ObserveEvent]
          event.eventName.name shouldBe ObserveEventNames.ExposureAborted.name
          ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
        }

        eventually {
          val event = testProbe.expectMessageType[ObserveEvent]
          event.eventName.name shouldBe ObserveEventNames.DataWriteStart.name
          event(ObserveEventKeys.filename).head shouldBe filename
          ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
        }

        eventually {
          val event = testProbe.expectMessageType[ObserveEvent]
          event.eventName.name shouldBe ObserveEventNames.DataWriteEnd.name
          event(ObserveEventKeys.filename).head shouldBe filename
          ExposureId(event(ObserveEventKeys.exposureId).head) shouldBe exposureId
        }

        val exposureAbortedRes = commandService.queryFinal(exposureAborted.runId).futureValue
        exposureAbortedRes shouldBe a[Completed]

        val shutdown = commandService.submit(Setup(testPrefix, Constants.Shutdown)).futureValue
        shutdown shouldBe a[Started]

        commandService.queryFinal(shutdown.runId).futureValue shouldBe a[Completed]
      }

      test(
        s"$detectorPrefix - behaviour should return Invalid when concurrent (Start Exposure) commands received | ESW-552, ESW-553"
      ) {
        implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)
        obsIdCount += 1
        val obsId        = ObsId(s"2020A-001-12$obsIdCount")
        val exposureId   = ExposureId(s"$obsId-IRIS-IMG-DRK1-0023")
        val testPrefix   = Prefix(IRIS, "darknight")
        val connection   = AkkaConnection(ComponentId(detectorPrefix, ComponentType.Assembly))
        val akkaLocation = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get
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

        val commandService: CommandService = assertAssemblyIsConfigured(obsId, exposureId, testPrefix, akkaLocation)

        val startExposure = Observe(testPrefix, Constants.StartExposure, Some(obsId))

        val exposureStarted  = commandService.submit(startExposure).futureValue
        val exposureStarted2 = commandService.submit(startExposure).futureValue
        exposureStarted shouldBe a[Started]
        exposureStarted2 shouldBe a[Invalid]
      }
    }
  }

  private def assertAssemblyIsConfigured(obsId: ObsId, exposureId: ExposureId, testPrefix: Prefix, akkaLocation: AkkaLocation) = {
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
        Constants.rampIntegrationTimeKey.set(1760)
      )
    )
    val configureStarted = commandService.submit(configure).futureValue
    configureStarted shouldBe a[Started]
    val finalConfigureRes = commandService.queryFinal(configureStarted.runId).futureValue
    finalConfigureRes shouldBe a[Completed]
    commandService
  }
}

package iris.ifsres

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.event.client.EventServiceFactory
import csw.event.client.models.EventStores.RedisStore
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.commands.{ControlCommand, Setup}
import csw.params.core.models.Id
import csw.params.events.Event
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS
import iris.ifsres.commands.SelectCommand
import iris.ifsres.models.ResWheelPosition
import Constants.IfsResAssemblyConnection
import ResWheelPosition._
import iris.ifsres.events.IfsPositionEvent

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object DemoApp {
  private implicit lazy val system: ActorSystem[Nothing] = ActorSystemFactory.remote(Behaviors.empty, "main")
  private implicit lazy val timeout: Timeout             = Timeout(1.minute)

  private lazy val locationService     = HttpLocationServiceFactory.makeLocalClient
  private lazy val redisStore          = RedisStore()
  private lazy val eventServiceFactory = new EventServiceFactory(redisStore)
  private lazy val eventService        = eventServiceFactory.make(locationService)
  private lazy val eventSubscriber     = eventService.defaultSubscriber

  private val sequencerPrefix = Prefix(IRIS, "darknight")

  System.setProperty("INTERFACE_NAME", "en0")

  def main(args: Array[String]): Unit =
    try {
      val imagerAssembly = Await.result(locationService.resolve(IfsResAssemblyConnection, 5.seconds), 6.seconds).get
      val commandService = CommandServiceFactory.make(imagerAssembly)

      subscribeToIfsPositionEvents()
      Thread.sleep(5000)
      moveCommandScenario(commandService, Mirror)
//      concurrentMoveCommandsScenario(commandService, R4000_H_K)
    }
    finally shutdown()

  private def moveCommandScenario(commandService: CommandService, target: ResWheelPosition): Unit = {
    val spectralResolutionSetup =
      Setup(sequencerPrefix, SelectCommand.Name, None).add(SelectCommand.SpectralResolutionKey.set(target.entryName))
    val initial = submitCommand(commandService, spectralResolutionSetup)
    queryFinal(commandService, initial.runId)
  }

  private def concurrentMoveCommandsScenario(commandService: CommandService, target: ResWheelPosition): Unit = {
    val spectralResolutionSetup =
      Setup(sequencerPrefix, SelectCommand.Name, None).add(SelectCommand.SpectralResolutionKey.set(target.entryName))
    val initial1 = submitCommand(commandService, spectralResolutionSetup)
    val initial2 = submitCommand(commandService, spectralResolutionSetup)
    queryFinal(commandService, initial1.runId)
    queryFinal(commandService, initial2.runId)
  }

  private def submitCommand(commandService: CommandService, command: ControlCommand) = {
    val response = Await.result(commandService.submit(command), timeout.duration)
    println(s"INITIAL RESPONSE: $response")
    response
  }

  private def queryFinal(commandService: CommandService, runId: Id) = {
    val response = Await.result(commandService.queryFinal(runId), timeout.duration)
    println(s"FINAL RESPONSE: $response")
    response
  }

  private def subscribeToIfsPositionEvents() =
    eventSubscriber
      .subscribe(Set(IfsPositionEvent.IfsResPositionEventKey))
      .runForeach(e => printImagerPositionEvent(e))

  private def printImagerPositionEvent(event: Event) = for {
    current <- event.paramType.get(IfsPositionEvent.CurrentPositionKey).flatMap(_.get(0))
    target  <- event.paramType.get(IfsPositionEvent.TargetPositionKey).flatMap(_.get(0))
  } yield println(s"$current, $target")

  private def shutdown(): Unit = {
    redisStore.redisClient.shutdown()
    system.terminate()
  }
}

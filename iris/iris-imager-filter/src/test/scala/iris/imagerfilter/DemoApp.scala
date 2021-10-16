package iris.imagerfilter

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.event.client.EventServiceFactory
import csw.event.client.models.EventStores.RedisStore
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.commands.CommandResponse.{SubmitResponse, isFinal}
import csw.params.commands.{ControlCommand, Setup}
import csw.params.events.Event
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS
import iris.imagerfilter.Constants.ImagerFilterAssemblyConnection
import iris.imagerfilter.commands.SelectCommand
import iris.imagerfilter.events.ImagerPositionEvent
import iris.imagerfilter.models.FilterWheelPosition
import iris.imagerfilter.models.FilterWheelPosition._

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
      val imagerAssembly = Await.result(locationService.resolve(ImagerFilterAssemblyConnection, 5.seconds), 6.seconds).get
      val commandService = CommandServiceFactory.make(imagerAssembly)

      subscribeToImagerPositionEvents()
      Thread.sleep(10000)
      moveCommandScenario(commandService, PaBeta)
//      concurrentMoveCommandsScenario(commandService, JCont)
    }
    finally shutdown()

  private def moveCommandScenario(commandService: CommandService, target: FilterWheelPosition): Unit = {
    val wheel1Setup = Setup(sequencerPrefix, SelectCommand.Name, None).add(SelectCommand.Wheel1Key.set(target.entryName))
    val initial     = submitCommand(commandService, wheel1Setup)
    queryFinal(commandService, initial)
  }

  private def concurrentMoveCommandsScenario(commandService: CommandService, target: FilterWheelPosition): Unit = {
    val wheel1Setup = Setup(sequencerPrefix, SelectCommand.Name, None).add(SelectCommand.Wheel1Key.set(target.entryName))
    val initial1    = submitCommand(commandService, wheel1Setup)
    val initial2    = submitCommand(commandService, wheel1Setup)
    queryFinal(commandService, initial1)
    queryFinal(commandService, initial2)
  }

  private def submitCommand(commandService: CommandService, command: ControlCommand) = {
    val response = Await.result(commandService.submit(command), timeout.duration)
    println(s"INITIAL RESPONSE: $response")
    response
  }

  private def queryFinal(cs: CommandService, res: SubmitResponse): SubmitResponse = {
    if (isFinal(res)) return res
    val response = Await.result(cs.queryFinal(res.runId), timeout.duration)
    println(s"FINAL RESPONSE: $response")
    response
  }

  private def subscribeToImagerPositionEvents() =
    eventSubscriber
      .subscribe(Set(ImagerPositionEvent.ImagerPositionEventKey))
      .runForeach(e => printImagerPositionEvent(e))

  private def printImagerPositionEvent(event: Event) = for {
    current <- event.paramType.get(ImagerPositionEvent.CurrentPositionKey).flatMap(_.get(0))
    target  <- event.paramType.get(ImagerPositionEvent.DemandPositionKey).flatMap(_.get(0))
    dark    <- event.paramType.get(ImagerPositionEvent.DarkKey).flatMap(_.get(0))
  } yield println(s"$current, $target, $dark")

  private def shutdown(): Unit = {
    redisStore.redisClient.shutdown()
    system.terminate()
  }
}

package iris.ifsscale

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
import iris.ifsscale.Constants.IfsScaleAssemblyConnection
import iris.ifsscale.commands.SelectCommand
import iris.ifsscale.events.IfsScaleEvent
import iris.ifsscale.models.ScaleLevel
import iris.ifsscale.models.ScaleLevel._

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
      val scaleAssembly  = Await.result(locationService.resolve(IfsScaleAssemblyConnection, 5.seconds), 6.seconds).get
      val commandService = CommandServiceFactory.make(scaleAssembly)

      subscribeToIfsScaleEvents()
      Thread.sleep(5000)
//      moveCommandScenario(commandService, S4)
      concurrentMoveCommandsScenario(commandService, S50)
    }
    finally shutdown()

  private def moveCommandScenario(commandService: CommandService, target: ScaleLevel): Unit = {
    val scaleSetup =
      Setup(sequencerPrefix, SelectCommand.Name, None).add(SelectCommand.ScaleKey.set(target.entryName))
    val initial = submitCommand(commandService, scaleSetup)
    queryFinal(commandService, initial)
  }

  private def concurrentMoveCommandsScenario(commandService: CommandService, target: ScaleLevel): Unit = {
    val scaleSetup =
      Setup(sequencerPrefix, SelectCommand.Name, None).add(SelectCommand.ScaleKey.set(target.entryName))
    val initial1 = submitCommand(commandService, scaleSetup)
    val initial2 = submitCommand(commandService, scaleSetup)
    queryFinal(commandService, initial1)
    queryFinal(commandService, initial2)
  }

  private def submitCommand(commandService: CommandService, command: ControlCommand) = {
    val response = Await.result(commandService.submit(command), timeout.duration)
    println(s"INITIAL RESPONSE: $response")
    response
  }

  private def queryFinal(commandService: CommandService, res: SubmitResponse): SubmitResponse = {
    if (isFinal(res)) return res
    val response = Await.result(commandService.queryFinal(res.runId), timeout.duration)
    println(s"FINAL RESPONSE: $response")
    response
  }

  private def subscribeToIfsScaleEvents() =
    eventSubscriber
      .subscribe(Set(IfsScaleEvent.IfsScaleEventKey))
      .runForeach(e => printScaleEvent(e))

  private def printScaleEvent(event: Event): Unit = {
    val current = event.paramType(IfsScaleEvent.CurrentScaleKey).head
    val target  = event.paramType(IfsScaleEvent.TargetScaleKey).head
    println(s"$current, $target")
  }

  private def shutdown(): Unit = {
    redisStore.redisClient.shutdown()
    system.terminate()
  }
}

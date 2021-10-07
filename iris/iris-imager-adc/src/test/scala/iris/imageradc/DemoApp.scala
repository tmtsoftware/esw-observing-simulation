package iris.imageradc

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
import iris.imageradc.Constants.ImagerADCAssemblyConnection
import iris.imageradc.commands.ADCCommand
import iris.imageradc.events.PrismCurrentEvent.ImagerADCCurrentEventKey
import iris.imageradc.events.PrismRetractEvent.{ImagerADCRetractEventKey, ImagerADCRetractEventName}
import iris.imageradc.events.PrismStateEvent.ImagerADCStateEventKey
import iris.imageradc.events.PrismTargetEvent.ImagerADCTargetEventKey
import iris.imageradc.events.{PrismCurrentEvent, PrismRetractEvent, PrismStateEvent, PrismTargetEvent}
import iris.imageradc.models.PrismPosition

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
      val imagerAssembly = Await.result(locationService.resolve(ImagerADCAssemblyConnection, 5.seconds), 6.seconds).get
      val commandService = CommandServiceFactory.make(imagerAssembly)

      subscribeToCurrentEvent()
      subscribeToRetractEvent()
      subscribeToTargetEvent()
      subscribeToStateEvent()
      Thread.sleep(10000)
      moveCommandScenario(commandService)
//      concurrentMoveCommandsScenario(commandService, R4000_H_K)
    }
    finally shutdown()

  private def moveCommandScenario(commandService: CommandService): Unit = {
    val InCommand =
      Setup(sequencerPrefix, ADCCommand.RetractSelect, None).add(PrismPosition.RetractKey.set(PrismPosition.IN.entryName))
//    println(submitCommand(commandService, InCommand))
    val initial = submitCommand(commandService, InCommand)
//    queryFinal(commandService, initial.runId)
    val FollowCommand =
      Setup(sequencerPrefix, ADCCommand.PrismFollow, None).add(ADCCommand.targetAngleKey.set(20.0))
    submitCommand(commandService, FollowCommand)

    Thread.sleep(10000)
    val StopCommand = Setup(sequencerPrefix, ADCCommand.PrismStop, None)
    submitCommand(commandService, StopCommand)
    println("***********************2nd follow command starts here****************")
    val Follow2Command =
      Setup(sequencerPrefix, ADCCommand.PrismFollow, None).add(ADCCommand.targetAngleKey.set(10.0))
    submitCommand(commandService, Follow2Command)
    Thread.sleep(10000)

    submitCommand(commandService, StopCommand)
  }

  private def concurrentMoveCommandsScenario(commandService: CommandService): Unit = {
//    val spectralResolutionSetup =
//      Setup(sequencerPrefix, SelectCommand.Name, None).add(SelectCommand.SpectralResolutionKey.set(target.entryName))
//    val initial1 = submitCommand(commandService, spectralResolutionSetup)
//    val initial2 = submitCommand(commandService, spectralResolutionSetup)
//    queryFinal(commandService, initial1.runId)
//    queryFinal(commandService, initial2.runId)
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
// ImagerADCRetractEventKey, ImagerADCCurrentEventKey, ImagerADCTargetEventKey
  private def subscribeToStateEvent() =
    eventSubscriber
      .subscribe(Set(ImagerADCStateEventKey))
      .runForeach(e => printPrismStateEvent(e))

  private def printPrismStateEvent(event: Event) = for {
    move     <- event.paramType.get(PrismStateEvent.moveKey).flatMap(_.get(0))
    onTarget <- event.paramType.get(PrismStateEvent.onTargetKey).flatMap(_.get(0))
  } yield println(s"Prism State: $move, OnTarget: $onTarget")

  private def subscribeToTargetEvent() =
    eventSubscriber
      .subscribe(Set(ImagerADCTargetEventKey))
      .runForeach(e => printPrismTargetEvent(e))

  private def printPrismTargetEvent(event: Event) =
    for {
      angle <- event.paramType.get(PrismTargetEvent.angleKey).flatMap(_.get(0))
    } yield println(s"Target Angle: $angle")


  private def subscribeToRetractEvent() =
    eventSubscriber
      .subscribe(Set(ImagerADCRetractEventKey))
      .runForeach(e => printPrismRetractEvent(e))

  private def printPrismRetractEvent(event: Event) = {
    println(s"event, $event")
    for {
      position <- event.paramType.get(PrismPosition.RetractKey).flatMap(_.get(0))
    } yield println(s"Retract position: $position")
  }

  private def subscribeToCurrentEvent() =
    eventSubscriber
      .subscribe(Set(ImagerADCCurrentEventKey))
      .runForeach(e => {
        println(e.paramSet.toString())
        printPrismCurrentEvent(e)
      })

  private def printPrismCurrentEvent(event: Event) = for {
    angle      <- event.paramType.get(PrismCurrentEvent.angleKey).flatMap(_.get(0))
    angleError <- event.paramType.get(PrismCurrentEvent.angleErrorKey).flatMap(_.get(0))
  } yield println(s"Current angle: $angle, Angle Error: $angleError")

  private def shutdown(): Unit = {
    redisStore.redisClient.shutdown()
    system.terminate()
  }
}

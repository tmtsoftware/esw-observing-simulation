package iris.imagerfilter

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import csw.command.client.CommandServiceFactory
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.Connection.AkkaConnection
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.commands.Setup
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS
import iris.imagerfilter.commands.SelectCommand

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object DemoApp {
  private implicit lazy val system: ActorSystem[Nothing] = ActorSystemFactory.remote(Behaviors.empty, "main")
  private implicit lazy val timeout: Timeout             = Timeout(1.minute)

  private lazy val locationService     = HttpLocationServiceFactory.makeLocalClient
  private val imagerAssemblyConnection = AkkaConnection(ComponentId(Prefix(IRIS, "imager.filter"), Assembly))

  System.setProperty("INTERFACE_NAME", "en0")

  def main(args: Array[String]): Unit = {
    try {
      val imagerAssembly = Await.result(locationService.resolve(imagerAssemblyConnection, 5.seconds), 6.seconds).get
      val cs             = CommandServiceFactory.make(imagerAssembly)

      val targetPosition  = "f7"
      val wheel1Setup     = Setup(Prefix("IRIS.darknight"), SelectCommand.Name, None).add(SelectCommand.Wheel1Key.set(targetPosition))
      val initialResponse = Await.result(cs.submit(wheel1Setup), 1.minute)
      println(initialResponse)

      val initialResponse2 = Await.result(cs.submit(wheel1Setup), 1.minute)
      println(initialResponse2)

      val finalResponse2 = Await.result(cs.queryFinal(initialResponse2.runId), 1.minute)
      println(finalResponse2)

      val finalResponse = Await.result(cs.queryFinal(initialResponse.runId), 1.minute)
      println(finalResponse)
    }
    finally system.terminate()
  }
}

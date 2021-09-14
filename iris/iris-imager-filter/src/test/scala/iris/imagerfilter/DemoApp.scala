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
  private implicit lazy val timeout: Timeout             = Timeout(20.seconds)

  private lazy val locationService     = HttpLocationServiceFactory.makeLocalClient
  private val imagerAssemblyConnection = AkkaConnection(ComponentId(Prefix(IRIS, "imager.filter"), Assembly))

  System.setProperty("INTERFACE_NAME", "en0")

  def main(args: Array[String]): Unit = {
    val imagerAssembly = Await.result(locationService.resolve(imagerAssemblyConnection, 5.seconds), 6.seconds).get
    val cs             = CommandServiceFactory.make(imagerAssembly)

    val wheel1Setup     = Setup(Prefix("IRIS.darknight"), SelectCommand.Name, None).add(SelectCommand.Wheel1Key.set("f7"))
    val initialResponse = Await.result(cs.submit(wheel1Setup), 10.seconds)

    println(initialResponse)
    val finalResponse = Await.result(cs.queryFinal(initialResponse.runId), 25.seconds)
    println(finalResponse)
  }
}

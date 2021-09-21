package iris.ifsres

import akka.actor.typed.ActorSystem

import java.time.Duration

case class ResWheelConfiguration(wheelDelay: Duration)

object ResWheelConfiguration {
  def apply(actorSystem: ActorSystem[_]): ResWheelConfiguration = {
    val config     = actorSystem.settings.config.getConfig("iris.ifs.res")
    val wheelDelay = config.getDuration("wheelDelay")
    new ResWheelConfiguration(wheelDelay)
  }
}

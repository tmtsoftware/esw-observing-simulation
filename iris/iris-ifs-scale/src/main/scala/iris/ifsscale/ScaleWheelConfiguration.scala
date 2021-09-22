package iris.ifsscale

import akka.actor.typed.ActorSystem

import java.time.Duration

case class ScaleWheelConfiguration(wheelDelay: Duration)

object ScaleWheelConfiguration {
  def apply(actorSystem: ActorSystem[_]): ScaleWheelConfiguration = {
    val config     = actorSystem.settings.config.getConfig("iris.ifs.scale")
    val wheelDelay = config.getDuration("wheelDelay")
    new ScaleWheelConfiguration(wheelDelay)
  }
}

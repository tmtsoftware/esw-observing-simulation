package iris.imagerfilter

import akka.actor.typed.ActorSystem

import java.time.Duration

case class FilterWheelConfiguration(wheelDelay: Duration)

object FilterWheelConfiguration {
  def apply(actorSystem: ActorSystem[_]): FilterWheelConfiguration = {
    val config     = actorSystem.settings.config.getConfig("iris.imager.filter")
    val wheelDelay = config.getDuration("wheelDelay")
    new FilterWheelConfiguration(wheelDelay)
  }
}

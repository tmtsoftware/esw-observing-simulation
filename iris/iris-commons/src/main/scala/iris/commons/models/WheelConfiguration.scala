package iris.commons.models

import com.typesafe.config.Config

import java.time.Duration

case class WheelConfiguration(wheelDelay: Duration)

object WheelConfiguration {
  def apply(config: Config): WheelConfiguration = {
    val wheelDelay = config.getDuration("wheelDelay")
    new WheelConfiguration(wheelDelay)
  }
}

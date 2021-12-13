package iris.imageradc.models

import com.typesafe.config.Config

import java.time.Duration

case class AssemblyConfiguration(
    retractSelectDelay: Duration,
    targetMovementDelay: Duration,
    targetMovementAngle: Double,
    toleranceAngle: Double
)

object AssemblyConfiguration {
  def apply(config: Config): AssemblyConfiguration = {
    val retractSelectDelay  = config.getDuration("retractSelectDelay")
    val targetMovementDelay = config.getDuration("targetMovementDelay")
    val targetMovementAngle = config.getDouble("targetMovementAngle")
    val toleranceAngle      = config.getDouble("toleranceAngle")
    new AssemblyConfiguration(retractSelectDelay, targetMovementDelay, targetMovementAngle, toleranceAngle)
  }
}

package iris.imageradc.commands

sealed trait PrismCommands

object PrismCommands {
  case object IN  extends PrismCommands
  case object OUT extends PrismCommands // PRISM_RETRACT
}

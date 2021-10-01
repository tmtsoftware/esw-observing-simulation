package iris.imageradc.commands
import csw.params.commands.CommandIssue.{MissingKeyIssue, ParameterValueOutOfRangeIssue}
import csw.params.commands.{CommandIssue, CommandName, Setup}
import csw.params.core.generics.GChoiceKey
import iris.imageradc.models.PrismPosition

object ADCCommand {
  val PrismFollow: CommandName   = CommandName("PRISM_FOLLOW")
  val PrismStop: CommandName     = CommandName("PRISM_STOP")
  val RetractSelect: CommandName = CommandName("RETRACT_SELECT")
  val PositionKey: GChoiceKey    = PrismPosition.makeChoiceKey("position")

  private def getPosition(setup: Setup, key: GChoiceKey): Either[CommandIssue, PrismPosition] = {
    val cmdName = setup.commandName.name
    for {
      param          <- setup.get(key).toRight(MissingKeyIssue(s"$PositionKey not found in command: $cmdName"))
      positionChoice <- param.get(0).toRight(ParameterValueOutOfRangeIssue(s"Command: $cmdName does not contain position"))
      position <- PrismPosition
        .withNameInsensitiveOption(positionChoice.name)
        .toRight(ParameterValueOutOfRangeIssue(s"$positionChoice is not a valid position"))
    } yield position
  }

  def getPrismPosition(setup: Setup): Either[CommandIssue, PrismPosition] =
    getPosition(setup, PositionKey)
}

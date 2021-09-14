package iris.imagerfilter.commands

import csw.params.commands.CommandIssue.{MissingKeyIssue, ParameterValueOutOfRangeIssue}
import csw.params.commands.{CommandIssue, CommandName, Setup}
import csw.params.core.generics.GChoiceKey
import csw.params.core.generics.KeyType.ChoiceKey
import iris.imagerfilter.models.FilterWheelPosition

object SelectCommand {
  val Name: CommandName     = CommandName("SELECT")
  val Wheel1Key: GChoiceKey = ChoiceKey.make("wheel1", FilterWheelPosition.choices)

  private def getTargetPosition(setup: Setup, key: GChoiceKey): Either[CommandIssue, FilterWheelPosition] = {
    val cmdName = setup.commandName.name
    for {
      param          <- setup.get(key).toRight(MissingKeyIssue(s"$Wheel1Key not found in command: $cmdName"))
      positionChoice <- param.get(0).toRight(ParameterValueOutOfRangeIssue(s"Command: $cmdName does not contain target position"))
      position <- FilterWheelPosition
        .withNameInsensitiveOption(positionChoice.name)
        .toRight(ParameterValueOutOfRangeIssue(s"$positionChoice is not a valid position"))
    } yield position
  }

  def getWheel1TargetPosition(setup: Setup): Either[CommandIssue, FilterWheelPosition] =
    getTargetPosition(setup, Wheel1Key)
}

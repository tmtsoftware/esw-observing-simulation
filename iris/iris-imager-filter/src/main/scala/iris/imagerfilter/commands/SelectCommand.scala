package iris.imagerfilter.commands

import csw.params.commands.CommandIssue.{MissingKeyIssue, ParameterValueOutOfRangeIssue}
import csw.params.commands.{CommandIssue, CommandName, Setup}
import csw.params.core.generics.Key
import csw.params.core.generics.KeyType.StringKey
import iris.imagerfilter.models.FilterWheelPosition

object SelectCommand {
  val Name: CommandName      = CommandName("SELECT")
  val Wheel1Key: Key[String] = StringKey.make("wheel1")

  private def getTargetPosition(setup: Setup, key: Key[String]): Either[CommandIssue, FilterWheelPosition] = {
    val cmdName = setup.commandName.name
    for {
      param       <- setup.get(key).toRight(MissingKeyIssue(s"$Wheel1Key not found in command: $cmdName"))
      positionStr <- param.get(0).toRight(ParameterValueOutOfRangeIssue(s"Command: $cmdName does not contain target position"))
      position <- FilterWheelPosition
        .withNameInsensitiveOption(positionStr)
        .toRight(ParameterValueOutOfRangeIssue(s"$positionStr is not a valid position"))
    } yield position
  }

  def getWheel1TargetPosition(setup: Setup): Either[CommandIssue, FilterWheelPosition] =
    getTargetPosition(setup, Wheel1Key)
}

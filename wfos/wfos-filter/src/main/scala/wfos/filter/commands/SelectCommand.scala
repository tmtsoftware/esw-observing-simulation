package wfos.filter.commands

import csw.params.commands.CommandIssue.{MissingKeyIssue, ParameterValueOutOfRangeIssue}
import csw.params.commands.{CommandIssue, CommandName, Setup}
import csw.params.core.generics.GChoiceKey
import wfos.filter.models.{BlueFilterWheelPosition, FilterWheelPosition, RedFilterWheelPosition}

abstract class SelectCommand {
  val Name: CommandName = CommandName("SELECT")
  val Wheel1Key: GChoiceKey
  def getWheel1TargetPosition(setup: Setup): Either[CommandIssue, FilterWheelPosition]
}

object RedSelectCommand extends SelectCommand {
  override val Wheel1Key: GChoiceKey = RedFilterWheelPosition.makeChoiceKey("wheel1")

  override def getWheel1TargetPosition(setup: Setup): Either[CommandIssue, FilterWheelPosition] = {
    val cmdName = setup.commandName.name
    for {
      param          <- setup.get(Wheel1Key).toRight(MissingKeyIssue(s"$Wheel1Key not found in command: $cmdName"))
      positionChoice <- param.get(0).toRight(ParameterValueOutOfRangeIssue(s"Command: $cmdName does not contain target position"))
      position <- RedFilterWheelPosition
        .withNameInsensitiveOption(positionChoice.name)
        .toRight(ParameterValueOutOfRangeIssue(s"$positionChoice is not a valid position"))
    } yield position
  }
}

object BlueSelectCommand extends SelectCommand {
  override val Wheel1Key: GChoiceKey = BlueFilterWheelPosition.makeChoiceKey("wheel1")

  override def getWheel1TargetPosition(setup: Setup): Either[CommandIssue, FilterWheelPosition] = {
    val cmdName = setup.commandName.name
    for {
      param          <- setup.get(Wheel1Key).toRight(MissingKeyIssue(s"$Wheel1Key not found in command: $cmdName"))
      positionChoice <- param.get(0).toRight(ParameterValueOutOfRangeIssue(s"Command: $cmdName does not contain target position"))
      position <- BlueFilterWheelPosition
        .withNameInsensitiveOption(positionChoice.name)
        .toRight(ParameterValueOutOfRangeIssue(s"$positionChoice is not a valid position"))
    } yield position
  }
}

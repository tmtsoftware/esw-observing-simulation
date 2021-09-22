package iris.ifsscale.commands

import csw.params.commands.CommandIssue.{MissingKeyIssue, ParameterValueOutOfRangeIssue}
import csw.params.commands.{CommandIssue, CommandName, Setup}
import csw.params.core.generics.GChoiceKey
import iris.ifsscale.models.ScaleLevel

object SelectCommand {
  val Name: CommandName    = CommandName("SELECT")
  val ScaleKey: GChoiceKey = ScaleLevel.makeChoiceKey("scale")

  private def getScale(setup: Setup, key: GChoiceKey): Either[CommandIssue, ScaleLevel] = {
    val cmdName = setup.commandName.name
    for {
      param       <- setup.get(key).toRight(MissingKeyIssue(s"$ScaleKey not found in command: $cmdName"))
      scaleChoice <- param.get(0).toRight(ParameterValueOutOfRangeIssue(s"Command: $cmdName does not contain target scale"))
      scale <- ScaleLevel
        .withNameInsensitiveOption(scaleChoice.name)
        .toRight(ParameterValueOutOfRangeIssue(s"$scaleChoice is not a valid scale level"))
    } yield scale
  }

  def getTargetScale(setup: Setup): Either[CommandIssue, ScaleLevel] =
    getScale(setup, ScaleKey)
}

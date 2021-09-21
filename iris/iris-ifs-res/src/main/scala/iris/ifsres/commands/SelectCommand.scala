package iris.ifsres.commands

import csw.params.commands.CommandIssue.{MissingKeyIssue, ParameterValueOutOfRangeIssue}
import csw.params.commands.{CommandIssue, CommandName, Setup}
import csw.params.core.generics.GChoiceKey
import iris.ifsres.models.ResWheelPosition

object SelectCommand {
  val Name: CommandName                 = CommandName("GRATING_SELECT")
  val SpectralResolutionKey: GChoiceKey = ResWheelPosition.makeChoiceKey("spectralResolution")

  private def getTargetPosition(setup: Setup, key: GChoiceKey): Either[CommandIssue, ResWheelPosition] = {
    val cmdName = setup.commandName.name
    for {
      param          <- setup.get(key).toRight(MissingKeyIssue(s"$SpectralResolutionKey not found in command: $cmdName"))
      positionChoice <- param.get(0).toRight(ParameterValueOutOfRangeIssue(s"Command: $cmdName does not contain target position"))
      position <- ResWheelPosition
        .withNameInsensitiveOption(positionChoice.name)
        .toRight(ParameterValueOutOfRangeIssue(s"$positionChoice is not a valid position"))
    } yield position
  }

  def getSpectralResolutionTargetPosition(setup: Setup): Either[CommandIssue, ResWheelPosition] =
    getTargetPosition(setup, SpectralResolutionKey)
}

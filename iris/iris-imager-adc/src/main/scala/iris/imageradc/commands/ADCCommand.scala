package iris.imageradc.commands
import csw.params.commands.CommandIssue.{MissingKeyIssue, ParameterValueOutOfRangeIssue}
import csw.params.commands.{CommandIssue, CommandName, Setup}
import csw.params.core.generics.KeyType.DoubleKey
import csw.params.core.generics.{GChoiceKey, Key}
import iris.imageradc.models.PrismPosition
import iris.imageradc.models.PrismPosition.RetractKey

object ADCCommand {
  val PrismFollow: CommandName    = CommandName("PRISM_FOLLOW")
  val PrismStop: CommandName      = CommandName("PRISM_STOP")
  val RetractSelect: CommandName  = CommandName("RETRACT_SELECT")
  val targetAngleKey: Key[Double] = DoubleKey.make("targetAngle")

  private def getPosition(setup: Setup, key: GChoiceKey): Either[CommandIssue, PrismPosition] = {
    val cmdName = setup.commandName.name
    for {
      param          <- setup.get(key).toRight(MissingKeyIssue(s"$RetractKey not found in command: $cmdName"))
      positionChoice <- param.get(0).toRight(ParameterValueOutOfRangeIssue(s"Command: $cmdName does not contain position"))
      position <- PrismPosition
        .withNameInsensitiveOption(positionChoice.name)
        .toRight(ParameterValueOutOfRangeIssue(s"$positionChoice is not a valid position"))
    } yield position
  }

  private def getTarget(setup: Setup, key: Key[Double]): Either[CommandIssue, Double] = {
    val cmdName = setup.commandName.name
    for {
      param       <- setup.get(key).toRight(MissingKeyIssue(s"$targetAngleKey not found in command: $cmdName"))
      targetAngle <- param.get(0).toRight(ParameterValueOutOfRangeIssue(s"Command: $cmdName does not contain position"))
    } yield targetAngle
  }

  def getPrismPosition(setup: Setup): Either[CommandIssue, PrismPosition] =
    getPosition(setup, RetractKey)

  def getTargetAngle(setup: Setup): Either[CommandIssue, Double] =
    getTarget(setup, targetAngleKey)
}

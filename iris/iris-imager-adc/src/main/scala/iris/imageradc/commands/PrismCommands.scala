package iris.imageradc.commands

import akka.actor.typed.ActorRef
import csw.params.commands.{Setup}
import csw.params.commands.CommandResponse.ValidateCommandResponse
import csw.params.core.models.Id
import iris.imageradc.models.PrismPosition

sealed trait PrismCommands

object PrismCommands {
  case class RETRACT_SELECT(runId: Id, position: PrismPosition)                              extends PrismCommands
  case class IS_VALID(runId: Id, command: Setup, replyTo: ActorRef[ValidateCommandResponse]) extends PrismCommands
  case class PRISM_FOLLOW(runId: Id, targetAngle: Double)                                    extends PrismCommands
  case class PRISM_STOP(runId: Id)                                                           extends PrismCommands
  case object MOVE_TARGET                                                                    extends PrismCommands
  case object MOVE_CURRENT                                                                   extends PrismCommands
  case object GOING_IN                                                                       extends PrismCommands
  case object GOING_OUT                                                                      extends PrismCommands
}

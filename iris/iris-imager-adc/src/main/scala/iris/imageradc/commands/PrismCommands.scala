package iris.imageradc.commands

import akka.actor.typed.ActorRef
import csw.params.commands.CommandResponse.ValidateCommandResponse
import csw.params.commands.Setup
import csw.params.core.models.Id
import iris.imageradc.models.PrismPosition

sealed trait PrismCommands

object PrismCommands {
  case class RetractSelect(runId: Id, position: PrismPosition)                              extends PrismCommands
  case class IsValid(runId: Id, command: Setup, replyTo: ActorRef[ValidateCommandResponse]) extends PrismCommands
  case class PrismFollow(targetAngle: Double)                                    extends PrismCommands
  case class PrismStop(runId: Id)                                                           extends PrismCommands
  case object FollowTarget                                                                   extends PrismCommands
  case object GoingIn                                                                       extends PrismCommands
  case object GoingOut                                                                      extends PrismCommands
}

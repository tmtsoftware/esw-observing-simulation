package wfos.filter.models

import akka.actor.typed.ActorRef
import csw.params.commands.CommandResponse.ValidateCommandResponse
import csw.params.core.models.Id

sealed trait WheelCommand

object WheelCommand {
  case class Move(target: FilterWheelPosition, runId: Id)                       extends WheelCommand
  case class IsValidMove(runId: Id, replyTo: ActorRef[ValidateCommandResponse]) extends WheelCommand
  case object MoveStep                                                          extends WheelCommand
}

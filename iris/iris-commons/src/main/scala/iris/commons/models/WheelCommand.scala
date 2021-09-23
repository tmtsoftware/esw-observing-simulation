package iris.commons.models

import akka.actor.typed.ActorRef
import csw.params.commands.CommandResponse.ValidateCommandResponse
import csw.params.core.models.Id

sealed trait WheelCommand[+T]

object WheelCommand {
  case class Move[T <: Position[T]](target: T, runId: Id)                       extends WheelCommand[T]
  case class IsValidMove(runId: Id, replyTo: ActorRef[ValidateCommandResponse]) extends WheelCommand[Nothing]
  case object MoveStep                                                          extends WheelCommand[Nothing]
}

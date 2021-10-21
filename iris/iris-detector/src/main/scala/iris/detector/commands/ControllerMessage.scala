package iris.detector.commands

import akka.actor.typed.ActorRef
import csw.params.commands.CommandName
import csw.params.commands.CommandResponse.ValidateCommandResponse
import csw.params.core.models.{ExposureId, Id}

sealed trait ControllerMessage

object ControllerMessage {
  case class IsValid(runId: Id, commandName: CommandName, replyTo: ActorRef[ValidateCommandResponse]) extends ControllerMessage

  case class Initialize(runId: Id) extends ControllerMessage
  case class ConfigureExposure(runId: Id, exposureId: ExposureId, filename: String, ramps: Int, rampIntegrationTime: Int)
      extends ControllerMessage
  case class StartExposure(
      runId: Id,
      replyTo: ActorRef[FitsMessage]
  )                                                          extends ControllerMessage
  case class ExposureInProgress(runId: Id, currentRamp: Int) extends ControllerMessage
  case class ExposureFinished(runId: Id)                     extends ControllerMessage
  case class AbortExposure(runId: Id)                        extends ControllerMessage
  case class Shutdown(runId: Id)                             extends ControllerMessage
}

case class FitsData(data: Array[Array[Int]]) {
  val dimensions: (Int, Int) = (data.length, data(0).length)
}

sealed trait FitsMessage

object FitsMessage {
  case class WriteData(runId: Id, data: FitsData, exposureId: ExposureId, filename: String) extends FitsMessage
}

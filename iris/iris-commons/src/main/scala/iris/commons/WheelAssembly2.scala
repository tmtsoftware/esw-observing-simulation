package iris.commons

import akka.Done
import akka.actor.typed.ActorSystem
import csw.framework.models.CswContext
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandIssue.AssemblyBusyIssue
import csw.params.commands.CommandResponse.{Accepted, Completed, Invalid, ValidateCommandResponse}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import iris.commons.models.{AssemblyConfiguration, Position}
import iris.commons.utils.Strand

import scala.async.Async._
import scala.concurrent.Future

abstract class WheelAssembly2[B <: Position[B]](cswContext: CswContext, configuration: AssemblyConfiguration) {
  private val timeServiceScheduler = cswContext.timeServiceScheduler
  private val crm                  = cswContext.commandResponseManager

  protected val name: String
  protected def publishPosition(current: B, target: B, dark: Boolean): Future[Done]
  private def unhandledMessage(state: String) = s"$name: Cannot accept command: moveStep in [$state] state"

  class WheelAssemblyStrand(init: B)(implicit actorSystem: ActorSystem[_]) extends Strand {

    private val log: Logger = cswContext.loggerFactory.getLogger

    private var currentState: WheelActions[B] = idle(init)

    def move(target: B, runId: Id): Unit                        = async(currentState.move(target, runId))
    def isValidMove(runId: Id): Future[ValidateCommandResponse] = async(currentState.isValidMove(runId))

    private def idle(current: B): WheelActions[B] = new WheelActions[B] {
      override def move(target: B, runId: Id): Unit                = moving(runId, current, target)
      override def isValidMove(runId: Id): ValidateCommandResponse = Accepted(runId)
      override def moveStep(): Unit                                = log.error(unhandledMessage("idle"))
    }

    private def moveBehavior(runId: Id, currentPos: B, targetPos: B): WheelActions[B] = new WheelActions[B] {
      override def move(target: B, runId: Id): Unit = {
        val errMsg = unhandledMessage("moving")
        log.error(errMsg)
        crm.updateCommand(Invalid(runId, AssemblyBusyIssue(errMsg)))
      }
      override def isValidMove(runId: Id): ValidateCommandResponse = {
        val errMsg = unhandledMessage("moving")
        log.error(errMsg)
        val issue = AssemblyBusyIssue(errMsg)
        Invalid(runId, issue)
      }
      override def moveStep(): Unit = {
        val nextPosition = currentPos.nextPosition(targetPos)
        if (nextPosition != targetPos) publishPosition(nextPosition, targetPos, dark = true)
        moving(runId, nextPosition, targetPos)
      }
    }

    private def moving(runId: Id, current: B, target: B): Unit = {
      if (current == target) {
        log.info(s"$name: target position: $currentState reached")
        publishPosition(current, target, dark = false)
        crm.updateCommand(Completed(runId))
        currentState = idle(current)
      }
      else {
        currentState = moveBehavior(runId, current, target)
        scheduleMoveStep(currentState)
      }
    }

    private def scheduleMoveStep(currentState: WheelActions[B]) =
      timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plus(configuration.movementDelay))) {
        async(currentState.moveStep())
      }
  }

  private trait WheelActions[T <: Position[T]] {
    def move(target: T, runId: Id): Unit
    def isValidMove(runId: Id): ValidateCommandResponse
    def moveStep(): Unit
  }
}

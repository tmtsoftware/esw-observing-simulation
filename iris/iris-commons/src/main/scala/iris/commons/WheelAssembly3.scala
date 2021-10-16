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

abstract class WheelAssembly3[B <: Position[B]](cswContext: CswContext, configuration: AssemblyConfiguration) {
  private val timeServiceScheduler = cswContext.timeServiceScheduler
  private val crm                  = cswContext.commandResponseManager

  protected val name: String
  protected def publishPosition(current: B, target: B, dark: Boolean): Future[Done]
  private def unhandledMessage(state: String) = s"$name: Cannot accept command: move in [$state] state"

  class WheelAssemblyStrand(init: B)(implicit actorSystem: ActorSystem[_]) extends Strand {

    private val log: Logger = cswContext.loggerFactory.getLogger

    private var currentState: State = State.Idle
    private var currentPosition: B  = init
    private var targetPosition: B   = _
    private var runId: Id           = _

    def move(target: B, runId: Id): Unit = async {
      currentState match {
        case State.Idle =>
          this.runId = runId
          this.targetPosition = target
          moving()
        case State.Moving =>
          val errMsg = unhandledMessage("moving")
          log.error(errMsg)
          crm.updateCommand(Invalid(runId, AssemblyBusyIssue(errMsg)))
      }
    }

    def isValidMove(runId: Id): Future[ValidateCommandResponse] = async {
      currentState match {
        case State.Idle => Accepted(runId)
        case State.Moving =>
          val errMsg = unhandledMessage("moving")
          log.error(errMsg)
          val issue = AssemblyBusyIssue(errMsg)
          Invalid(runId, issue)
      }
    }

    private def moveStep(): Unit = {
      currentState match {
        case State.Idle => log.error(unhandledMessage("idle"))
        case State.Moving =>
          val nextPosition = currentPosition.nextPosition(targetPosition)
          if (nextPosition != targetPosition) publishPosition(nextPosition, targetPosition, dark = true)
          currentPosition = nextPosition
          moving()
      }
    }

    private def moving(): Unit = {
      if (currentPosition == targetPosition) {
        log.info(s"$name: target position: $currentState reached")
        publishPosition(currentPosition, targetPosition, dark = false)
        crm.updateCommand(Completed(runId))
        currentState = State.Idle
      }
      else {
        currentState = State.Moving
        timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plus(configuration.movementDelay))) {
          async(moveStep())
        }
      }
    }

    private sealed trait State
    private object State {
      case object Idle   extends State
      case object Moving extends State
    }
  }
}

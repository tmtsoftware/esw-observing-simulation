package iris.imagerfilter

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.params.commands.CommandResponse._
import csw.params.commands.ControlCommand
import csw.params.core.generics.Parameter
import csw.params.core.models.Id
import csw.params.events.{EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime

import scala.concurrent.ExecutionContextExecutor

/**
 * Domain specific logic should be written in below handlers.
 * This handlers gets invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to Irishcd,
 * This will be first validated in the supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw/commons/framework.html
 */
class ImagerFilterHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._
  implicit val ec: ExecutionContextExecutor = ctx.executionContext
  private val log                           = loggerFactory.getLogger
  private var currentPosition               = "f1"
  private val positions                     = List("f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9", "f10", "f11", "f12", "f13", "f14", "f15", "f16")
  override def initialize(): Unit = {
    log.info("Initializing imager.filter...")

  }

  def getMoves(current: String, target: String): List[String] = {
    val currentIndex = positions.indexOf(current)
    val targetIndex  = positions.indexOf(target)
    positions.slice(currentIndex + 1, targetIndex + 1)
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = Accepted(runId)

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = {
//    val parameter: Parameter[String] = StringKey.make("wheel1").set("f1")
    val parameter: Parameter[_] = controlCommand.paramSet.find(_.keyName == "wheel1").get
    parameter.values.head match {
      case s: String => {
        val moves = getMoves(currentPosition, s)

      }
    }

    //f1, f4

    //f1,f2
    //f2,f3
    //f3,f4
    //f4,f4

    (currentPosition, parameter.values.head) match {
      case ("f1", "f2") | ("f2", "f1") =>
      case ("f1", "f2")                =>
      case ("f1", "f2")                =>
    }

    parameter.values.head match {
      case s: String =>
        println(s)
        currentPosition = s
        val event = SystemEvent(Prefix("IRIS.imager.filter"), EventName("filterWheel"))
        eventService.defaultPublisher.publish(event.madd(parameter))
    }
    Started(runId)
    //sleep
    Completed(runId)
  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {}

  override def onShutdown(): Unit = {}

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}

}

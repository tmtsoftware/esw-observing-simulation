package iris.imager.detector

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Scheduler}
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.params.commands.CommandResponse.{Accepted, Invalid, Started, SubmitResponse}
import csw.params.commands._
import csw.params.core.models.{ExposureId, Id}
import csw.params.events.ObserveEventKeys
import csw.time.core.models.UTCTime
import iris.imager.detector.commands.ControllerMessage._
import iris.imager.detector.commands.{ControllerMessage, FitsMessage}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext}

class ImagerDetectorHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {
  import cswCtx._
  implicit val ec: ExecutionContext         = ctx.executionContext
  private val log                           = loggerFactory.getLogger
  private val config: Config                = ConfigFactory.load().getConfig(cswCtx.componentInfo.prefix.toString().toLowerCase())
  implicit private val scheduler: Scheduler = ctx.system.scheduler
  val fitsActor: ActorRef[FitsMessage] = ctx.spawnAnonymous(new FitsActor(cswCtx, config).setup)
  val controller: ActorRef[ControllerMessage] =
    ctx.spawnAnonymous(new ControllerActor(cswCtx, config).uninitialized)

  override def initialize(): Unit = log.info("Initializing imager.adc...")

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def validateCommand(runId: Id, controlCommand: ControlCommand): CommandResponse.ValidateCommandResponse = {
    if (
      controlCommand.maybeObsId.isEmpty &&
      controlCommand.commandName != Constants.Initialize &&
      controlCommand.commandName != Constants.Shutdown
    ) {
      return Invalid(runId, CommandIssue.WrongParameterTypeIssue("obsId not found"))
    }
    controlCommand match {
      case setup: Setup     => validateSetup(runId, setup)
      case observe: Observe => validateObserve(runId, observe)
    }
  }

  private def validateObserve(runId: Id, observe: Observe) = {
    val timeout: FiniteDuration = 1.seconds
    implicit val value: Timeout = Timeout(timeout)
    Await.result(controller ? (IsValid(runId, observe.commandName, _)), timeout)
  }

  private def validateSetup(runId: Id, setup: Setup): CommandResponse.ValidateCommandResponse = {
    val timeout: FiniteDuration = 1.seconds
    implicit val value: Timeout = Timeout(timeout)

    val validateCommandResponse = setup.commandName match {
      case Constants.LoadConfiguration =>
        (for {
          _ <- validateExposureIdKey(runId, setup)
          _ <- setup.get(ObserveEventKeys.filename).toRight(Invalid(runId, CommandIssue.WrongParameterTypeIssue("filename not found")))
          _ <- setup.get(Constants.rampsKey).toRight(Invalid(runId, CommandIssue.WrongParameterTypeIssue("ramps not found")))
          _ <- setup
            .get(Constants.rampIntegrationTimeKey)
            .toRight(Invalid(runId, CommandIssue.WrongParameterTypeIssue("rampIntegrationTime not found")))
        } yield Accepted(runId)) match {
          case Left(invalid)   => invalid
          case Right(accepted) => accepted
        }
      case _ =>
        Accepted(runId)
    }
    validateCommandResponse match {
      case _: Accepted =>
        Await.result(controller ? (IsValid(runId, setup.commandName, _)), 1.seconds)
      case invalid => invalid
    }
  }

  private def validateExposureIdKey(runId: Id, setup: Setup) = {
    val invalid = Invalid(runId, CommandIssue.WrongParameterTypeIssue("Invalid Exposure id"))
    try {
      setup.get(ObserveEventKeys.exposureId).map(x => ExposureId(x.head)).toRight(invalid)
    }
    catch {
      case _: Exception => Left(invalid)
    }
  }

  override def onSubmit(runId: Id, controlCommand: ControlCommand): CommandResponse.SubmitResponse = controlCommand match {
    case setup: Setup     => onSetup(runId, setup)
    case observe: Observe => onObserve(runId, observe)
  }

  def onSetup(runId: Id, setup: Setup): SubmitResponse = setup.commandName match {
    case Constants.Initialize =>
      controller ! Initialize(runId)
      Started(runId)
    case Constants.Shutdown =>
      controller ! Shutdown(runId)
      Started(runId)
    case Constants.LoadConfiguration =>
      val exposureId      = ExposureId(setup(ObserveEventKeys.exposureId).head)
      val filename        = setup(ObserveEventKeys.filename).head
      val ramps           = setup(Constants.rampsKey).head
      val integrationTime = setup(Constants.rampIntegrationTimeKey).head
      controller ! ConfigureExposure(runId, exposureId, filename, ramps, integrationTime)
      Started(runId)
    case cmd =>
      Invalid(runId, CommandIssue.UnsupportedCommandIssue(s"${cmd.name} is not a supported Setup command"))
  }

  def onObserve(runId: Id, observe: Observe): SubmitResponse = observe.commandName match {
    case Constants.StartExposure =>
      controller ! StartExposure(runId, fitsActor)
      Started(runId)
    case Constants.AbortExposure =>
      controller ! AbortExposure(runId)
      Started(runId)
    case cmd =>
      Invalid(runId, CommandIssue.UnsupportedCommandIssue(s"${cmd.name} is not a supported Observe command"))
  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = ???

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = ???

  override def onOperationsMode(): Unit = ???

  override def onShutdown(): Unit = ???

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}

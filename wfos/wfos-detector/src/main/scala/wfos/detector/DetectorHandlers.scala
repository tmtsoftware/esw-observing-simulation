package wfos.detector

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Scheduler}
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.params.commands.CommandResponse.{Invalid, Started, SubmitResponse}
import csw.params.commands._
import csw.params.core.generics.Parameter
import csw.params.core.models.{ExposureId, Id}
import csw.params.events.ObserveEventKeys
import csw.time.core.models.UTCTime
import wfos.detector.commands.ControllerMessage._
import wfos.detector.commands.{ControllerMessage, FitsMessage}

import scala.concurrent.Await
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class DetectorHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {
  import cswCtx._
  private implicit val scheduler: Scheduler = ctx.system.scheduler
  private val log                           = loggerFactory.getLogger
  private val config: Config                = ConfigFactory.load().getConfig(cswCtx.componentInfo.prefix.toString().toLowerCase())

  private val fitsActor: ActorRef[FitsMessage]        = ctx.spawnAnonymous(new FitsActor(cswCtx, config).setup)
  private val controller: ActorRef[ControllerMessage] = ctx.spawnAnonymous(new ControllerActor(cswCtx, config).uninitialized)
  private val timeout: FiniteDuration                 = 1.seconds
  private implicit val value: Timeout                 = Timeout(timeout)

  override def initialize(): Unit = log.info(s"Initializing ${cswCtx.componentInfo.prefix}...")

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  override def validateCommand(runId: Id, controlCommand: ControlCommand): CommandResponse.ValidateCommandResponse = {
    controlCommand match {
      case setup: Setup     => validateSetup(runId, setup)
      case observe: Observe => validateObserve(runId, observe)
    }
  }

  private def validateObserve(runId: Id, command: Observe) = {
    command.commandName match {
      case Constants.StartExposure =>
        command.maybeObsId match {
          case Some(_) => sendIsValid(runId, command)
          case None    => Invalid(runId, CommandIssue.WrongParameterTypeIssue("obsId not found"))
        }
      case cmd => Invalid(runId, CommandIssue.UnsupportedCommandIssue(s"$cmd is not a valid observe command"))
    }

  }

  private def sendIsValid(runId: Id, command: ControlCommand) = {
    Await.result(controller ? (IsValid(runId, command.commandName, _)), timeout)
  }

  private def validateSetup(runId: Id, setup: Setup): CommandResponse.ValidateCommandResponse = {
    setup.commandName match {
      case Constants.StartExposure =>
        Invalid(runId, CommandIssue.UnsupportedCommandIssue("StartExposure is not a valid setup command"))
      case Constants.Initialize | Constants.Shutdown =>
        sendIsValid(runId, setup)
      case Constants.LoadConfiguration =>
        val issueOrAccepted = for {
          _     <- setup.maybeObsId.toRight(CommandIssue.WrongParameterTypeIssue("obsId not found"))
          _     <- validateExposureIdKey(setup)
          _     <- setup.get(ObserveEventKeys.filename).toRight(CommandIssue.WrongParameterTypeIssue("filename not found"))
          ramps <- setup.get(Constants.rampsKey).toRight(CommandIssue.WrongParameterTypeIssue("ramps not found"))
          _     <- isGreaterThan(ramps, Constants.minRampsValue)
          rampIntegrationTime <- setup
            .get(Constants.rampIntegrationTimeKey)
            .toRight(CommandIssue.WrongParameterTypeIssue("rampIntegrationTime not found"))
          _ <- isGreaterThan(rampIntegrationTime, Constants.minRampIntegrationTime)
        } yield sendIsValid(runId, setup)
        issueOrAccepted.fold(Invalid(runId, _), identity)
      case Constants.AbortExposure =>
        setup.maybeObsId match {
          case Some(_) => sendIsValid(runId, setup)
          case None    => Invalid(runId, CommandIssue.WrongParameterTypeIssue("obsId not found"))
        }
      case cmd => Invalid(runId, CommandIssue.UnsupportedCommandIssue(s"$cmd is not a valid setup command"))
    }
  }

  private def isGreaterThan(parameter: Parameter[Int], minVal: Int) = {
    if (parameter.head >= minVal) Right(parameter)
    else Left(CommandIssue.WrongParameterTypeIssue(s"${parameter.keyName} should be >= $minVal"))
  }

  private def validateExposureIdKey(setup: Setup) =
    try {
      setup
        .get(ObserveEventKeys.exposureId)
        .map(x => ExposureId(x.head))
        .toRight(CommandIssue.MissingKeyIssue("ExposureId not found"))
    }
    catch {
      case ex: Exception => Left(CommandIssue.WrongCommandTypeIssue(ex.getMessage))
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

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}

  override def onShutdown(): Unit = {}

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}
}

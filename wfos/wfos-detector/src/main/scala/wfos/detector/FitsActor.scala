package wfos.detector

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.Config
import csw.command.client.CommandResponseManager
import csw.event.api.scaladsl.EventPublisher
import csw.framework.models.CswContext
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandResponse.Completed
import csw.params.events.OpticalDetectorEvent
import wfos.detector.commands.FitsMessage._
import nom.tam.fits.{Fits, FitsFactory}
import nom.tam.util.BufferedFile
import wfos.detector.commands.{FitsData, FitsMessage}

class FitsActor(cswContext: CswContext, config: Config) {
  val crm: CommandResponseManager    = cswContext.commandResponseManager
  val eventPublisher: EventPublisher = cswContext.eventService.defaultPublisher
  val log: Logger                    = cswContext.loggerFactory.getLogger

  def setup: Behavior[FitsMessage] = Behaviors.receiveMessage { case WriteData(runId, data, exposureId, filename) =>
    val prefix = cswContext.componentInfo.prefix
    eventPublisher.publish(OpticalDetectorEvent.dataWriteStart(prefix, exposureId, filename))
    writeData(data, filename)
    crm.updateCommand(Completed(runId))
    eventPublisher.publish(OpticalDetectorEvent.dataWriteEnd(prefix, exposureId, filename))
    Behaviors.same
  }

  private def writeData(data: FitsData, filename: String): Unit = {
    if (config.getBoolean("writeDataToFile")) {
      val fits = new Fits()
      fits.addHDU(FitsFactory.hduFactory(data.data))
      val bf = new BufferedFile(filename, "rw")
      fits.write(bf)
      bf.close()
      log.info(s"Data has been written to $filename")
    }
  }
}

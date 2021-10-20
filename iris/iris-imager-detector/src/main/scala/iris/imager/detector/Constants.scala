package iris.imager.detector

import csw.params.commands.CommandName
import csw.params.core.generics.{Key, KeyType}

object Constants {
  val Initialize: CommandName        = CommandName("INIT")
  val LoadConfiguration: CommandName = CommandName("LOAD_CONFIGURATION")
  val StartExposure: CommandName     = CommandName("START_EXPOSURE")
  val AbortExposure: CommandName     = CommandName("ABORT_EXPOSURE")
  val Shutdown: CommandName          = CommandName("SHUTDOWN")

  // keys
  val obsIdKey: Key[String]            = KeyType.StringKey.make("obsId")
  val exposureIdKey: Key[String]       = KeyType.StringKey.make("exposureId")
  val filenameKey: Key[String]         = KeyType.StringKey.make("filename")
  val rampsKey: Key[Int]               = KeyType.IntKey.make("ramps")
  val rampIntegrationTimeKey: Key[Int] = KeyType.IntKey.make("rampIntegrationTime")

}

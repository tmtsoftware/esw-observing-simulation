import java.io.FileReader
import java.util.Properties

import sbt._

import scala.util.control.NonFatal

object Libs {
  val ScalaVersion = "2.13.8"

  val `scalatest`                = "org.scalatest"          %% "scalatest"                % "3.2.11"    //Apache License 2.0
  val `scala-async`              = "org.scala-lang.modules" %% "scala-async"              % "1.0.1" //BSD 3-clause "New" or "Revised" License
  val `junit-4-13`               = "org.scalatestplus"      %% "junit-4-13"               % "3.2.10.0"
  val `akka-actor-testkit-typed` = "com.typesafe.akka"      %% "akka-actor-testkit-typed" % "2.6.18"
  val `nom-tam-fits`             = "gov.nasa.gsfc.heasarc"   % "nom-tam-fits"             % "1.15.2"
  val `tmt-test-reporter`        = "com.github.tmtsoftware" %% "rtm"                      % "0.3.0"

}

object CSW {

  // If you want to change CSW version, then update "csw.version" property in "build.properties" file
  // Same "csw.version" property should be used while running the "csw-services",
  // this makes sure that CSW library dependency and CSW services version is in sync
  val Version: String = {
    var reader: FileReader = null
    try {
      val properties = new Properties()
      reader = new FileReader("project/build.properties")
      properties.load(reader)
      val version = properties.getProperty("csw.version")
      println(s"[info]] Using CSW version [$version] ***********")
      version
    }
    catch {
      case NonFatal(e) =>
        e.printStackTrace()
        throw e
    }
    finally reader.close()
  }

  val `csw-framework` = "com.github.tmtsoftware.csw" %% "csw-framework" % Version
  val `csw-testkit`   = "com.github.tmtsoftware.csw" %% "csw-testkit"   % Version
}

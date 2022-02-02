import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.{url, _}

object Common extends AutoPlugin {
  override def trigger: PluginTrigger      = allRequirements

  val storyReport: Boolean                 = sys.props.get("generateStoryReport").contains("true")

  private val reporterOptions: Seq[Tests.Argument] =
    if (storyReport)
      Seq(
        Tests.Argument(TestFrameworks.ScalaTest, "-oDF", "-C", "tmt.test.reporter.TestReporter"),
        Tests.Argument(TestFrameworks.JUnit, "-v", "-a")
      )
    else Seq(Tests.Argument("-oDF"), Tests.Argument(TestFrameworks.JUnit, "-v", "-a"))

  override def requires: Plugins = JvmPlugin

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    organization := "com.github.tmtsoftware.esw-observing-simulation",
    organizationName := "TMT",
    scalaVersion := "2.13.8",
    organizationHomepage := Some(url("http://www.tmt.org")),
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-Xlint",
      "-Ywarn-dead-code",
      //-W Options
      "-Wconf:any:warning-verbose",
      //-X Options
      "-Xlint:_,-missing-interpolator",
      "-Xcheckinit",
      "-Xasync"
    ),
    Compile / doc / javacOptions ++= Seq("-Xdoclint:none"),
    Test / testOptions ++= reporterOptions,
    resolvers += "jitpack" at "https://jitpack.io",
    version := "0.1.0-SNAPSHOT",
    fork := true,
    Test / parallelExecution := false,
    autoCompilerPlugins := true,
    if (formatOnCompile) scalafmtOnCompile := true else scalafmtOnCompile := false
  )

  private def formatOnCompile = sys.props.get("format.on.compile") match {
    case Some("false") ⇒ false
    case _             ⇒ true
  }
}

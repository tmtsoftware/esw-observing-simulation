import org.scalafmt.sbt.ScalafmtPlugin.autoImport.{scalafmtConfig, scalafmtOnCompile}
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.{url, _}

object Common extends AutoPlugin {
  private val enableFatalWarnings: Boolean = sys.props.get("enableFatalWarnings").contains("true")
  override def trigger: PluginTrigger      = allRequirements

  private val enableCoverage: Boolean      = sys.props.get("enableCoverage").contains("true")
  val MaybeCoverage: Plugins = if (enableCoverage) Coverage else Plugins.empty
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
    scalaVersion := Libs.ScalaVersion,
    organizationHomepage := Some(url("http://www.tmt.org")),
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      "-deprecation"
    ),
    Compile / doc / javacOptions ++= Seq("-Xdoclint:none"),
    Test / testOptions ++= reporterOptions,
    resolvers += "jitpack" at "https://jitpack.io",
    version := "0.3.0",
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

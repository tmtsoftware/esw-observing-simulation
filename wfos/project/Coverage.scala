import sbt._

object Coverage extends AutoPlugin {
  import scoverage.ScoverageSbtPlugin
  import ScoverageSbtPlugin.autoImport._

  override def requires: Plugins = ScoverageSbtPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    coverageEnabled := true,
    coverageMinimumStmtTotal := 70,
    coverageFailOnMinimum := true,
    coverageHighlighting := true,
    coverageOutputCobertura := true,
    coverageOutputXML := true
  )

}

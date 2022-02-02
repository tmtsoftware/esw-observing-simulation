import Common._
ThisBuild / scalafmtConfig := file("../.scalafmt.conf")

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(
  `wfos-filter`,
  `wfos-wfosdeploy`,
  `wfos-detector`
)

lazy val `wfos-root` = project
  .in(file("."))
  .aggregate(aggregatedProjects: _*)

// assembly module
lazy val `wfos-filter` = project
  .enablePlugins(MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.WfosAssemblies
  )

// deploy module
lazy val `wfos-wfosdeploy` = project
  .dependsOn(
    `wfos-filter`,
    `wfos-detector`
  )
  .enablePlugins(CswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.WfosDeploy
  )

lazy val `wfos-detector` = project
  .in(file("wfos-detector"))
  .enablePlugins(MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.WfosDetector
  )

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(
  `wfos-red-filter`,
  `wfos-wfosdeploy`,
  `wfos-commons`
)

lazy val `wfos-root` = project
  .in(file("."))
  .aggregate(aggregatedProjects: _*)

// assembly module
lazy val `wfos-red-filter` = project
  .dependsOn(`wfos-commons`)
  .settings(
    libraryDependencies ++= Dependencies.WfosAssemblies
  )

// deploy module
lazy val `wfos-wfosdeploy` = project
  .dependsOn(
    `wfos-red-filter`
  )
  .enablePlugins(CswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.WfosDeploy
  )

//common module
lazy val `wfos-commons` = project
  .settings(
    libraryDependencies ++= Dependencies.WfosCommons
  )

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(
  `iris-imager-filter`,
  `iris-irisdeploy`
)

lazy val `iris-root` = project
  .in(file("."))
  .aggregate(aggregatedProjects: _*)

// assembly module
lazy val `iris-imager-filter` = project
  .settings(
    libraryDependencies ++= Dependencies.ImagerFilter
  )

// deploy module
lazy val `iris-irisdeploy` = project
  .dependsOn(
    `iris-imager-filter`
  )
  .enablePlugins(CswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.IrisDeploy
  )

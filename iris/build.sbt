lazy val aggregatedProjects: Seq[ProjectReference] = Seq(
  `iris-imager-filter`,
  `iris-irisdeploy`,
  `iris-ifs-res`,
  `iris-ifs-scale`,
  `iris-commons`
)

lazy val `iris-root` = project
  .in(file("."))
  .aggregate(aggregatedProjects: _*)

// assembly module
lazy val `iris-imager-filter` = project
  .dependsOn(`iris-commons`)
  .settings(
    libraryDependencies ++= Dependencies.ImagerFilter
  )

// assembly2 module
lazy val `iris-ifs-res` = project
  .dependsOn(`iris-commons`)
  .settings(
    libraryDependencies ++= Dependencies.ImagerFilter
  )

// assembly3 module
lazy val `iris-ifs-scale` = project
  .in(file("iris-ifs-scale"))
  .dependsOn(`iris-commons`)
  .settings(
    libraryDependencies ++= Dependencies.ImagerFilter
  )

// deploy module
lazy val `iris-irisdeploy` = project
  .dependsOn(
    `iris-imager-filter`,
    `iris-ifs-res`,
    `iris-ifs-scale`
  )
  .enablePlugins(CswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.IrisDeploy
  )

//common module
lazy val `iris-commons` = project
  .settings(
    libraryDependencies ++= Dependencies.IrisCommons
  )

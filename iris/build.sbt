ThisBuild / scalacOptions ++= Seq(
  "-Xasync"
)

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(
  `iris-imager-filter`,
  `iris-irisdeploy`,
  `iris-ifs-res`,
  `iris-ifs-scale`,
  `iris-imager-adc`,
  `iris-commons`
)

lazy val `iris-root` = project
  .in(file("."))
  .aggregate(aggregatedProjects: _*)

// assembly module
lazy val `iris-imager-filter` = project
  .dependsOn(`iris-commons`)
  .settings(
    libraryDependencies ++= Dependencies.IrisAssemblies
  )

// assembly2 module
lazy val `iris-ifs-res` = project
  .dependsOn(`iris-commons`)
  .settings(
    libraryDependencies ++= Dependencies.IrisAssemblies
  )

// assembly3 module
lazy val `iris-ifs-scale` = project
  .in(file("iris-ifs-scale"))
  .dependsOn(`iris-commons`)
  .settings(
    libraryDependencies ++= Dependencies.IrisAssemblies
  )
// assembly4 module
lazy val `iris-imager-adc` = project
  .in(file("iris-imager-adc"))
  .dependsOn(`iris-commons`)
  .settings(
    libraryDependencies ++= Dependencies.IrisAssemblies
  )

// deploy module
lazy val `iris-irisdeploy` = project
  .dependsOn(
    `iris-imager-filter`,
    `iris-ifs-res`,
    `iris-ifs-scale`,
    `iris-imager-adc`
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

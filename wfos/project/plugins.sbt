//addSbtPlugin("org.scalastyle"                   %% "scalastyle-sbt-plugin"     % "1.0.0") // not scala 3 ready

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.4")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.3.1")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")


resolvers += "jitpack" at "https://jitpack.io"
libraryDependencies += "com.github.tmtsoftware" % "sbt-docs" % "0.7.1"

classpathTypes += "maven-plugin"

scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-deprecation"
)


addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")


resolvers += "jitpack" at "https://jitpack.io"
libraryDependencies += "com.github.tmtsoftware" % "sbt-docs" % "0.5.0"
//addSbtPlugin("org.scalastyle"                   %% "scalastyle-sbt-plugin"     % "1.0.0") // not scala 3 ready

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.4")


resolvers += "jitpack" at "https://jitpack.io"
libraryDependencies += "com.github.tmtsoftware" % "sbt-docs" % "0.7.1"

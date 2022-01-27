name := "integration"

version := "0.1"

scalaVersion := "2.13.6"
resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += ("com.github.tmtsoftware.esw"                      %% "esw-testkit"     % "180274e")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "iris-irisdeploy" % "390fa6d")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "wfos-wfosdeploy" % "390fa6d")
Test / parallelExecution := false
scalafmtConfig := file("../.scalafmt.conf")

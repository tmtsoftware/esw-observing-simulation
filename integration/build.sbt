name := "integration"

version := "0.1"

scalaVersion := "2.13.6"
resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += ("com.github.tmtsoftware.esw"                      %% "esw-testkit"     % "bb819c0")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "iris-irisdeploy" % "edab6c6")
Test / parallelExecution := false

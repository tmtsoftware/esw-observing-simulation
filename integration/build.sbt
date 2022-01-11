name := "integration"

version := "0.1"

scalaVersion := "2.13.6"
resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += ("com.github.tmtsoftware.esw"                      %% "esw-testkit"     % "cd601b9")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "iris-irisdeploy" % "5ce3799")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "wfos-wfosdeploy" % "5ce3799")
Test / parallelExecution := false

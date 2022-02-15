
name := "integration"

libraryDependencies += ("com.github.tmtsoftware.esw"                      %% "esw-testkit"     % "202f084a74")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "iris-irisdeploy" % "7cab663")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "wfos-wfosdeploy" % "7cab663")
libraryDependencies += ("com.github.tmtsoftware" %% "rtm" % "0.3.0")
scalafmtConfig := file("../.scalafmt.conf")


name := "integration"

libraryDependencies += ("com.github.tmtsoftware.esw"                      %% "esw-testkit"     % "0.4.0")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "iris-irisdeploy" % "d2f6a55")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "wfos-wfosdeploy" % "d2f6a55")
libraryDependencies += ("com.github.tmtsoftware" %% "rtm" % "0.3.0")
scalafmtConfig := file("../.scalafmt.conf")

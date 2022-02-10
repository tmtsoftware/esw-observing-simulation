
name := "integration"

libraryDependencies += ("com.github.tmtsoftware.esw"                      %% "esw-testkit"     % "f3d073f")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "iris-irisdeploy" % "ec59149")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "wfos-wfosdeploy" % "ec59149")
libraryDependencies += ("com.github.tmtsoftware" %% "rtm" % "0.3.0")
scalafmtConfig := file("../.scalafmt.conf")

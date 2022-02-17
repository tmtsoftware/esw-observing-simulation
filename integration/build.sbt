
name := "integration"

libraryDependencies += ("com.github.tmtsoftware.esw"                      %% "esw-testkit"     % "cae47ca")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "iris-irisdeploy" % "8c01615e")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "wfos-wfosdeploy" % "8c01615e")
libraryDependencies += ("com.github.tmtsoftware" %% "rtm" % "0.3.0")
scalafmtConfig := file("../.scalafmt.conf")

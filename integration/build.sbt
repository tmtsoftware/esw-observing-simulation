name := "integration"

libraryDependencies += ("com.github.tmtsoftware.esw"                      %% "esw-testkit"     % "1ea12a9")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "iris-irisdeploy" % "b263cc3")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "wfos-wfosdeploy" % "b263cc3")
libraryDependencies += ("com.github.tmtsoftware"                          %% "rtm"             % "0.3.0")
scalafmtConfig                                                            := file("../.scalafmt.conf")

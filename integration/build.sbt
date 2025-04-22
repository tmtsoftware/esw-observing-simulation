name := "integration"

libraryDependencies += ("com.github.tmtsoftware.esw"                      %% "esw-testkit"     % "f9cbac5")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "iris-irisdeploy" % "7bd94fb")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "wfos-wfosdeploy" % "7bd94fb")
libraryDependencies += ("com.github.tmtsoftware.rtm"                          %% "rtm"             % "0.4.1")
scalafmtConfig                                                            := file("../.scalafmt.conf")


name := "integration"

libraryDependencies += ("com.github.tmtsoftware.esw"                      %% "esw-testkit"     % "0.5.0")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "iris-irisdeploy" % "2d061ad")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "wfos-wfosdeploy" % "2d061ad")
libraryDependencies += ("com.github.tmtsoftware"                          %% "rtm"             % "0.3.0")
scalafmtConfig                                                            := file("../.scalafmt.conf")

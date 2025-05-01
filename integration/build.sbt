name := "integration"

libraryDependencies += ("com.github.tmtsoftware.esw"                      %% "esw-testkit"     % "v1.0.0")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "iris-irisdeploy" % "v1.0.0-RC2")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "wfos-wfosdeploy" % "v1.0.0-RC2")
libraryDependencies += ("com.github.tmtsoftware.rtm"                          %% "rtm"             % "0.4.1")
scalafmtConfig                                                            := file("../.scalafmt.conf")


name := "integration"

libraryDependencies += ("com.github.tmtsoftware.esw"                      %% "esw-testkit"     % "1.0.0-RC7")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "iris-irisdeploy" % "cc725ab")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "wfos-wfosdeploy" % "cc725ab")
libraryDependencies += ("com.github.tmtsoftware.rtm"                          %% "rtm"             % "0.4.1")
scalafmtConfig                                                            := file("../.scalafmt.conf")


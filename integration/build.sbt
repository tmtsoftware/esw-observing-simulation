name := "integration"

libraryDependencies += ("com.github.tmtsoftware.esw"                      %% "esw-testkit"     % "bab4a64")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "iris-irisdeploy" % "87655c9")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "wfos-wfosdeploy" % "87655c9")
libraryDependencies += ("com.github.tmtsoftware.rtm"                          %% "rtm"             % "d520268")
scalafmtConfig                                                            := file("../.scalafmt.conf")


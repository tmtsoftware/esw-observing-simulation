name := "integration"

libraryDependencies += ("com.github.tmtsoftware.esw"                      %% "esw-testkit"     % "f93d3c5")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "iris-irisdeploy" % "c39b347")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "wfos-wfosdeploy" % "c39b347")
libraryDependencies += ("com.github.tmtsoftware"                          %% "rtm"             % "0.3.0")
scalafmtConfig                                                            := file("../.scalafmt.conf")

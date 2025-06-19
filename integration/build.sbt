name := "integration"

libraryDependencies += ("com.github.tmtsoftware.esw"                      %% "esw-testkit"     % "1.0.2")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "iris-irisdeploy" % "44c4238")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "wfos-wfosdeploy" % "44c4238")
libraryDependencies += ("com.github.tmtsoftware.rtm"                          %% "rtm"             % "0.4.3")
scalafmtConfig                                                            := file("../.scalafmt.conf")


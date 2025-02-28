name := "integration"

libraryDependencies += ("com.github.tmtsoftware.esw"                      %% "esw-testkit"     % "bab4a64")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "iris-irisdeploy" % "d368bb6")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "wfos-wfosdeploy" % "d368bb6")
//libraryDependencies += ("com.github.tmtsoftware"                          %% "rtm"             % "b7997a9")
scalafmtConfig                                                            := file("../.scalafmt.conf")


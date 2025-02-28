name := "integration"

libraryDependencies += ("com.github.tmtsoftware.esw"                      %% "esw-testkit"     % "bab4a64")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "iris-irisdeploy" % "87655c9")
libraryDependencies += ("com.github.tmtsoftware.esw-observing-simulation" %% "wfos-wfosdeploy" % "87655c9")
//libraryDependencies += ("com.github.tmtsoftware"                          %% "rtm"             % "b7997a9")
scalafmtConfig                                                            := file("../.scalafmt.conf")


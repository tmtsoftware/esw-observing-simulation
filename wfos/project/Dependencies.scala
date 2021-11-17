import sbt._

object Dependencies {

  val WfosAssemblies = Seq(
    CSW.`csw-framework`,
    CSW.`csw-testkit`               % Test,
    Libs.`scalatest`                % Test,
    Libs.`junit-4-13`               % Test,
    Libs.`akka-actor-testkit-typed` % Test
  )

  val WfosDeploy = Seq(
    CSW.`csw-framework`,
    CSW.`csw-testkit` % Test
  )
  val WfosDetector = Seq(Libs.`nom-tam-fits`) ++ WfosAssemblies
}

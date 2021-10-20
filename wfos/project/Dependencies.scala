import sbt._

object Dependencies {

  val WfosAssemblies = Seq(
    CSW.`csw-testkit`               % Test,
    Libs.`scalatest`                % Test,
    Libs.`junit-4-13`               % Test,
    Libs.`akka-actor-testkit-typed` % Test
  )

  val WfosDeploy = Seq(
    CSW.`csw-framework`,
    CSW.`csw-testkit` % Test
  )

  val WfosCommons = Seq(CSW.`csw-framework`)
}

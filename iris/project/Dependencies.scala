import sbt._

object Dependencies {

  val IrisAssemblies = Seq(
    CSW.`csw-testkit`               % Test,
    Libs.`scalatest`                % Test,
    Libs.`junit-4-13`               % Test,
    Libs.`akka-actor-testkit-typed` % Test,
    Libs.`tmt-test-reporter`        % Test
  )

  val IrisDeploy = Seq(
    CSW.`csw-framework`,
    CSW.`csw-testkit` % Test
  )

  val IrisCommons = Seq(CSW.`csw-framework`)

  val IrisDetector = Seq(Libs.`nom-tam-fits`) ++ IrisAssemblies
}

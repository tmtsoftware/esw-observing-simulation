import sbt._

object Dependencies {

  val ImagerFilter = Seq(
    CSW.`csw-framework`,
    CSW.`csw-testkit` % Test,
    Libs.`scalatest`  % Test,
    Libs.`junit-4-13` % Test
  )

  val IrisDeploy = Seq(
    CSW.`csw-framework`,
    CSW.`csw-testkit` % Test
  )

  val IrisCommons = Seq(CSW.`csw-framework`)
}

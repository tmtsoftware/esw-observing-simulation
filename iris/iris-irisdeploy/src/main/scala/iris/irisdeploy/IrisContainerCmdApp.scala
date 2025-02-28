package iris.irisdeploy

import csw.framework.deploy.containercmd.ContainerCmd
import csw.prefix.models.Subsystem.IRIS

object IrisContainerCmdApp {
  def main(args: Array[String]): Unit = {
    ContainerCmd.start("iris_container_cmd_app", IRIS, args)
  }
}

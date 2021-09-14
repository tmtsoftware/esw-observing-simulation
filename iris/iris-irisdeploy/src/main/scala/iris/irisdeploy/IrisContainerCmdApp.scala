package iris.irisdeploy

import csw.framework.deploy.containercmd.ContainerCmd
import csw.prefix.models.Subsystem

object IrisContainerCmdApp extends App {

  ContainerCmd.start("iris_container_cmd_app", Subsystem.withNameInsensitive("IRIS"), args)

}

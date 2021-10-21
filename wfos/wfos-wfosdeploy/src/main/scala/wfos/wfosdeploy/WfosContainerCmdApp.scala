package wfos.wfosdeploy

import csw.framework.deploy.containercmd.ContainerCmd
import csw.prefix.models.Subsystem.WFOS

object WfosContainerCmdApp extends App {
  ContainerCmd.start("wfos_container_cmd_app", WFOS, args)
}

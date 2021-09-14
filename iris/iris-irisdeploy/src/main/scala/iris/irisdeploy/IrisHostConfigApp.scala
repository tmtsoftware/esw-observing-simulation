package iris.irisdeploy

import csw.framework.deploy.hostconfig.HostConfig
import csw.prefix.models.Subsystem

object IrisHostConfigApp extends App {

  HostConfig.start("iris_host_config_app", Subsystem.withNameInsensitive("IRIS"), args)

}

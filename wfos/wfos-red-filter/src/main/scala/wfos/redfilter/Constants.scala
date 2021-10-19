package wfos.redfilter

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.Connection.AkkaConnection
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.WFOS

object Constants {
  val RedFilterAssemblyPrefix: Prefix             = Prefix(WFOS, "red.filter")
  val RedFilterAssemblyConnection: AkkaConnection = AkkaConnection(ComponentId(RedFilterAssemblyPrefix, Assembly))
}

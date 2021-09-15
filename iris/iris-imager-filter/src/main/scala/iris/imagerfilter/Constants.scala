package iris.imagerfilter

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.Connection.AkkaConnection
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS

object Constants {
  val ImagerFilterAssemblyPrefix: Prefix             = Prefix(IRIS, "imager.filter")
  val ImagerFilterAssemblyConnection: AkkaConnection = AkkaConnection(ComponentId(ImagerFilterAssemblyPrefix, Assembly))
}

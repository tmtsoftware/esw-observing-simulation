package iris.ifsscale

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.Connection.PekkoConnection
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS

object Constants {
  val IfsScaleAssemblyPrefix: Prefix              = Prefix(IRIS, "ifs.scale")
  val IfsScaleAssemblyConnection: PekkoConnection = PekkoConnection(ComponentId(IfsScaleAssemblyPrefix, Assembly))
}

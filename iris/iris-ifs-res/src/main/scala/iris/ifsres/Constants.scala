package iris.ifsres

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.Connection.PekkoConnection
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS

object Constants {
  val IfsResAssemblyPrefix: Prefix              = Prefix(IRIS, "ifs.res")
  val IfsResAssemblyConnection: PekkoConnection = PekkoConnection(ComponentId(IfsResAssemblyPrefix, Assembly))
}

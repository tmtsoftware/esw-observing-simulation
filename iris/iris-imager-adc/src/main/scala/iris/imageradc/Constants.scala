package iris.imageradc

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.Connection.AkkaConnection
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS

object Constants {
  val ImagerADCAssemblyPrefix: Prefix             = Prefix(IRIS, "imager.adc")
  val ImagerADCAssemblyConnection: AkkaConnection = AkkaConnection(ComponentId(ImagerADCAssemblyPrefix, Assembly))
}

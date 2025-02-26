package iris.imageradc

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.Connection.PekkoConnection
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{IRIS, TCS}

object Constants {
  val ImagerADCAssemblyPrefix: Prefix              = Prefix(IRIS, "imager.adc")
  val TCSPointingKernelPrefix: Prefix              = Prefix(TCS, "PointingKernelAssembly")
  val ImagerADCAssemblyConnection: PekkoConnection = PekkoConnection(ComponentId(ImagerADCAssemblyPrefix, Assembly))
}

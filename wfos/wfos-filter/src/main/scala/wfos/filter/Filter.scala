package wfos.filter

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.Connection.AkkaConnection
import csw.prefix.models.Prefix

object Filter {
  def getConnection(filterPrefix: Prefix): AkkaConnection = AkkaConnection(ComponentId(filterPrefix, Assembly))
}

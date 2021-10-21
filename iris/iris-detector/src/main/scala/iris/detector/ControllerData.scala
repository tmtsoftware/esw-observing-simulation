package iris.detector

import csw.params.core.models.ExposureId

case class ControllerData(filename: String, exposureId: ExposureId, ramps: Int, rampIntegrationTime: Int)

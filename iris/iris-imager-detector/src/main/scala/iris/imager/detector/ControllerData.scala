package iris.imager.detector

import csw.params.core.models.ExposureId

case class ControllerData(filename: String, exposureId: ExposureId, ramps: Int, rampIntegrationTime: Int)

package iris.detector

import csw.params.core.models.ExposureId

case class ControllerData(filename: String, exposureId: ExposureId, ramps: Int, rampIntegrationTime: Int, currentRamp: Int) {
  def incrementRamp(): ControllerData = ControllerData(filename, exposureId, ramps, rampIntegrationTime, currentRamp + 1)
  def resetRamp(): ControllerData     = ControllerData(filename, exposureId, ramps, rampIntegrationTime, 0)
}

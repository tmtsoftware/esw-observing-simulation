package iris.imageradc

import scala.math.BigDecimal.RoundingMode

class PrismAngle(initialAngle: BigDecimal, defaultVelocity: BigDecimal, targetVelocity: BigDecimal, tolerance: BigDecimal) {

  private val totalTimeToCoverToleratedDistance = 3
  private var _currentAngle: BigDecimal         = round(initialAngle)
  private var _targetAngle: BigDecimal          = 0
  private var velocity: BigDecimal              = calculateDiff(_targetAngle)

  def target: BigDecimal = _targetAngle
  def currentAngle: BigDecimal  = _currentAngle

  def setTarget(target: BigDecimal): Unit = {
    _targetAngle = round(target)
    velocity = calculateDiff(target)
  }

  def nextCurrent(): Unit = {
    if (_currentAngle < _targetAngle) _currentAngle += velocity
    if ((_targetAngle - _currentAngle) <= tolerance) velocity = calculateDiff(_targetAngle)
  }


  def nextTarget(): Unit = {
    if (_currentAngle == _targetAngle) _targetAngle += targetVelocity
  }
  private def round(value: BigDecimal) = {
    (360 + (value % 360).setScale(1, RoundingMode.DOWN)) % 360
  }

  private def calculateDiff(target: BigDecimal) = {
    val roundedTarget = round(target)
    val distance      = roundedTarget - _currentAngle
    if (distance.abs > tolerance) round(distance / totalTimeToCoverToleratedDistance) else defaultVelocity
  }
}

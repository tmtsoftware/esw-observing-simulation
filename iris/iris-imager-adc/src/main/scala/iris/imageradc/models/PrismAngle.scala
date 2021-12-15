package iris.imageradc.models

import scala.math.BigDecimal.RoundingMode

class PrismAngle(initialAngle: BigDecimal, defaultSpeed: BigDecimal, targetVelocity: BigDecimal, tolerance: BigDecimal) {

  private val totalTimeToCoverToleratedDistance = 3
  private var _currentAngle: BigDecimal         = round(initialAngle)
  private var _targetAngle: BigDecimal          = 0
  private var velocity: BigDecimal              = calculateVelocity(_targetAngle)
  private def defaultVelocity                   = if (_targetAngle < currentAngle) defaultSpeed * -1 else defaultSpeed

  def target: BigDecimal       = _targetAngle
  def currentAngle: BigDecimal = _currentAngle

  def setTarget(target: BigDecimal): Unit = {
    _targetAngle = round(target)
    velocity = calculateVelocity(_targetAngle)
  }

  def nextCurrent(): Unit = {
    if (_currentAngle != _targetAngle) _currentAngle = round(_currentAngle + velocity)
    if ((_targetAngle - _currentAngle).abs <= tolerance) velocity = calculateVelocity(_targetAngle)
  }

  private def getToFirstDecimalPoint(value: BigDecimal) = (value % 360).setScale(1, RoundingMode.DOWN)
  private def round(value: BigDecimal) = {
    (360 + getToFirstDecimalPoint(value)) % 360
  }

  private def calculateVelocity(target: BigDecimal) = {
    val roundedTarget = round(target)
    val distance      = roundedTarget - _currentAngle
    if (distance.abs > tolerance) getToFirstDecimalPoint(distance / totalTimeToCoverToleratedDistance) else defaultVelocity
  }
}

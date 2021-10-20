package wfos.commons.models

import enumeratum.EnumEntry

trait Position[T <: Position[T]] extends EnumEntry {
  def nextPosition(target: T): T
}

trait LinearPosition[T <: LinearPosition[T]] extends Position[T] {
  protected def getIndexOf(currentPos: Position[T]): Int
  protected def nextPosition(step: Int): T

  final def nextPosition(target: T): T = nextPosition(nextIndexDiff(target))

  private final def nextIndexDiff(target: T): Int = {
    val currId   = getIndexOf(this)
    val targetId = getIndexOf(target)

    if (this == target) 0
    else if (currId > targetId) -1
    else +1
  }
}

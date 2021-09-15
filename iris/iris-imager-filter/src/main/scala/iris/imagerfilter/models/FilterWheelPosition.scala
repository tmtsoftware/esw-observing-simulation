package iris.imagerfilter.models

import csw.params.core.generics.GChoiceKey
import csw.params.core.generics.KeyType.ChoiceKey
import csw.params.core.models.Choices
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

sealed abstract class FilterWheelPosition(override val entryName: String) extends EnumEntry {
  def nextPosition(target: FilterWheelPosition): FilterWheelPosition = nextPosition(step(target))

  def nextPosition(step: Int): FilterWheelPosition = {
    val currId = FilterWheelPosition.values.indexOf(this)
    val nextId = currId + step
    if (nextId < 0 || nextId >= FilterWheelPosition.values.length) this
    else FilterWheelPosition.values(nextId)
  }

  def step(target: FilterWheelPosition): Int = {
    val currId   = FilterWheelPosition.indexOf(this)
    val targetId = FilterWheelPosition.indexOf(target)

    if (this == target) 0
    else if (currId > targetId) -1
    else +1
  }
}

object FilterWheelPosition extends Enum[FilterWheelPosition] {
  override def values: IndexedSeq[FilterWheelPosition] = findValues

  private lazy val choices: Choices              = Choices.from(FilterWheelPosition.values.map(_.entryName): _*)
  def makeChoiceKey(keyName: String): GChoiceKey = ChoiceKey.make(keyName, choices)

  case object F1  extends FilterWheelPosition("f1")
  case object F2  extends FilterWheelPosition("f2")
  case object F3  extends FilterWheelPosition("f3")
  case object F4  extends FilterWheelPosition("f4")
  case object F5  extends FilterWheelPosition("f5")
  case object F6  extends FilterWheelPosition("f6")
  case object F7  extends FilterWheelPosition("f7")
  case object F8  extends FilterWheelPosition("f8")
  case object F9  extends FilterWheelPosition("f9")
  case object F10 extends FilterWheelPosition("f10")
  case object F11 extends FilterWheelPosition("f11")
  case object F12 extends FilterWheelPosition("f12")
  case object F13 extends FilterWheelPosition("f13")
  case object F14 extends FilterWheelPosition("f14")
  case object F15 extends FilterWheelPosition("f15")
  case object F16 extends FilterWheelPosition("f16")
}

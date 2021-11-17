package wfos.filter.models

import csw.params.core.generics.GChoiceKey
import csw.params.core.generics.KeyType.ChoiceKey
import csw.params.core.models.Choices
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

sealed trait FilterWheelPosition extends EnumEntry {
  protected def getIndexOf(currentPos: FilterWheelPosition): Int
  protected def nextPosition(step: Int): FilterWheelPosition

  final def nextPosition(target: FilterWheelPosition): FilterWheelPosition = nextPosition(nextIndexDiff(target))

  private final def nextIndexDiff(target: FilterWheelPosition): Int = {
    val currId   = getIndexOf(this)
    val targetId = getIndexOf(target)

    if (this == target) 0
    else if (currId > targetId) -1
    else +1
  }
}

sealed abstract class RedFilterWheelPosition(override val entryName: String) extends FilterWheelPosition {

  override def getIndexOf(currentPos: FilterWheelPosition): Int =
    RedFilterWheelPosition.values.indexOf(currentPos)

  override def nextPosition(step: Int): RedFilterWheelPosition = {
    val currId = getIndexOf(this)
    val nextId = currId + step
    if (nextId < 0 || nextId >= RedFilterWheelPosition.values.length) this
    else RedFilterWheelPosition.values(nextId)
  }

}

sealed abstract class BlueFilterWheelPosition(override val entryName: String) extends FilterWheelPosition {

  override def getIndexOf(currentPos: FilterWheelPosition): Int =
    BlueFilterWheelPosition.values.indexOf(currentPos)

  override def nextPosition(step: Int): BlueFilterWheelPosition = {
    val currId = getIndexOf(this)
    val nextId = currId + step
    if (nextId < 0 || nextId >= BlueFilterWheelPosition.values.length) this
    else BlueFilterWheelPosition.values(nextId)
  }

}

object RedFilterWheelPosition extends Enum[RedFilterWheelPosition] {
  override def values: IndexedSeq[RedFilterWheelPosition] = findValues

  private lazy val choices: Choices              = Choices.from(values.map(_.entryName): _*)
  def makeChoiceKey(keyName: String): GChoiceKey = ChoiceKey.make(keyName, choices)

  case object RPrime      extends RedFilterWheelPosition("r'")
  case object IPrime      extends RedFilterWheelPosition("i'")
  case object ZPrime      extends RedFilterWheelPosition("z'")
  case object FusedSilica extends RedFilterWheelPosition("fused-silica")
}

object BlueFilterWheelPosition extends Enum[BlueFilterWheelPosition] {
  override def values: IndexedSeq[BlueFilterWheelPosition] = findValues

  private lazy val choices: Choices              = Choices.from(values.map(_.entryName): _*)
  def makeChoiceKey(keyName: String): GChoiceKey = ChoiceKey.make(keyName, choices)

  case object UPrime      extends BlueFilterWheelPosition("u'")
  case object GPrime      extends BlueFilterWheelPosition("g'")
  case object FusedSilica extends BlueFilterWheelPosition("fused-silica")
}

package iris.imageradc.models

import csw.params.core.generics.GChoiceKey
import csw.params.core.generics.KeyType.ChoiceKey
import csw.params.core.models.Choices
import enumeratum.Enum
import iris.commons.models.Position

import scala.collection.immutable.IndexedSeq

sealed class PrismPosition(override val entryName: String) extends Position[PrismPosition] {
  override def nextPosition(target: PrismPosition): PrismPosition = target
}

object PrismPosition extends Enum[PrismPosition] {
  override def values: IndexedSeq[PrismPosition] = findValues

  private lazy val choices: Choices              = Choices.from(PrismPosition.values.map(_.entryName): _*)
  def makeChoiceKey(keyName: String): GChoiceKey = ChoiceKey.make(keyName, choices)

  case object IN  extends PrismPosition("IN")
  case object OUT  extends PrismPosition("OUT")
}

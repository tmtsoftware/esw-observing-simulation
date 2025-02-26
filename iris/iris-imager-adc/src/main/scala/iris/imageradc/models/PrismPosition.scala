package iris.imageradc.models

import csw.params.core.generics.GChoiceKey
import csw.params.core.generics.KeyType.ChoiceKey
import csw.params.core.models.Choices
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

sealed abstract class PrismPosition(override val entryName: String) extends EnumEntry

object PrismPosition extends Enum[PrismPosition] {
  override def values: IndexedSeq[PrismPosition] = findValues
  val RetractKey: GChoiceKey                     = PrismPosition.makeChoiceKey("position")
  private lazy val choices: Choices              = Choices.from(PrismPosition.values.map(_.entryName)*)
  def makeChoiceKey(keyName: String): GChoiceKey = ChoiceKey.make(keyName, choices)

  case object IN  extends PrismPosition("IN")
  case object OUT extends PrismPosition("OUT")
}

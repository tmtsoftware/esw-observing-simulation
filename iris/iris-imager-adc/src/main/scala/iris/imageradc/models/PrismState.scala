package iris.imageradc.models

import csw.params.core.generics.GChoiceKey
import csw.params.core.generics.KeyType.ChoiceKey
import csw.params.core.models.Choices
import enumeratum.{Enum, EnumEntry}

sealed class PrismState(override val entryName: String) extends EnumEntry

object PrismState extends Enum[PrismState] {
  override def values: IndexedSeq[PrismState] = findValues

  private lazy val choices: Choices              = Choices.from(PrismState.values.map(_.entryName)*)
  def makeChoiceKey(keyName: String): GChoiceKey = ChoiceKey.make(keyName, choices)

  case object FOLLOWING extends PrismState("FOLLOWING")
  case object STOPPED   extends PrismState("STOPPED")
}

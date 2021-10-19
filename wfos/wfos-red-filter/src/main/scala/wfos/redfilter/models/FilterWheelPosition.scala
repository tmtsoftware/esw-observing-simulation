package wfos.redfilter.models

import csw.params.core.generics.GChoiceKey
import csw.params.core.generics.KeyType.ChoiceKey
import csw.params.core.models.Choices
import enumeratum.Enum
import wfos.commons.models.{LinearPosition, Position}

import scala.collection.immutable.IndexedSeq

sealed abstract class FilterWheelPosition(override val entryName: String) extends LinearPosition[FilterWheelPosition] {

  override def getIndexOf(currentPos: Position[FilterWheelPosition]): Int = FilterWheelPosition.values.indexOf(currentPos)

  override def nextPosition(step: Int): FilterWheelPosition = {
    val currId = getIndexOf(this)
    val nextId = currId + step
    if (nextId < 0 || nextId >= FilterWheelPosition.values.length) this
    else FilterWheelPosition.values(nextId)
  }

}

object FilterWheelPosition extends Enum[FilterWheelPosition] {
  override def values: IndexedSeq[FilterWheelPosition] = findValues

  private lazy val choices: Choices              = Choices.from(FilterWheelPosition.values.map(_.entryName): _*)
  def makeChoiceKey(keyName: String): GChoiceKey = ChoiceKey.make(keyName, choices)

  case object Z        extends FilterWheelPosition("Z")
  case object Y        extends FilterWheelPosition("Y")
  case object J        extends FilterWheelPosition("J")
  case object H        extends FilterWheelPosition("H")
  case object K        extends FilterWheelPosition("K")
  case object Ks       extends FilterWheelPosition("Ks")
  case object HKNotch  extends FilterWheelPosition("H+K notch")
  case object CO       extends FilterWheelPosition("CO")
  case object BrGamma  extends FilterWheelPosition("BrGamma")
  case object PaBeta   extends FilterWheelPosition("PaBeta")
  case object H2       extends FilterWheelPosition("H2")
  case object FeII     extends FilterWheelPosition("FeII")
  case object HeI      extends FilterWheelPosition("HeI")
  case object CaIITrip extends FilterWheelPosition("CaII Trip")
  case object JCont    extends FilterWheelPosition("J Cont")
  case object HCont    extends FilterWheelPosition("H Cont")
  case object KCont    extends FilterWheelPosition("K Cont")
}

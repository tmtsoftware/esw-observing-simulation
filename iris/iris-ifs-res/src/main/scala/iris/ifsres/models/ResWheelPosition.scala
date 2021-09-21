package iris.ifsres.models

import csw.params.core.generics.GChoiceKey
import csw.params.core.generics.KeyType.ChoiceKey
import csw.params.core.models.Choices
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

sealed abstract class ResWheelPosition(override val entryName: String) extends EnumEntry {
  def nextPosition(target: ResWheelPosition): ResWheelPosition = nextPosition(step(target))

  def nextPosition(step: Int): ResWheelPosition = {
    val currId = ResWheelPosition.values.indexOf(this)
    val nextId = currId + step
    if (nextId < 0 || nextId >= ResWheelPosition.values.length) this
    else ResWheelPosition.values(nextId)
  }

  def step(target: ResWheelPosition): Int = {
    val currId   = ResWheelPosition.indexOf(this)
    val targetId = ResWheelPosition.indexOf(target)

    if (this == target) 0
    else if (currId > targetId) -1
    else +1
  }
}

object ResWheelPosition extends Enum[ResWheelPosition] {
  override def values: IndexedSeq[ResWheelPosition] = findValues

  private lazy val choices: Choices              = Choices.from(ResWheelPosition.values.map(_.entryName): _*)
  def makeChoiceKey(keyName: String): GChoiceKey = ChoiceKey.make(keyName, choices)

  case object R4000_Z     extends ResWheelPosition("4000-Z")
  case object R4000_Y     extends ResWheelPosition("4000-Y")
  case object R4000_J     extends ResWheelPosition("4000-J")
  case object R4000_H     extends ResWheelPosition("4000-H")
  case object R4000_K     extends ResWheelPosition("4000-K")
  case object R4000_H_K   extends ResWheelPosition("4000-H+K")
  case object R8000_Z     extends ResWheelPosition("8000-Z")
  case object R8000_Y     extends ResWheelPosition("8000-Y")
  case object R8000_J     extends ResWheelPosition("8000-J")
  case object R8000_H     extends ResWheelPosition("8000-H")
  case object R8000_Kn1_3 extends ResWheelPosition("8000-Kn1-3")
  case object R8000_Kn4_5 extends ResWheelPosition("8000-Kn4-5")
  case object R8000_K_b   extends ResWheelPosition("8000-Kbb")
  case object R10000_Z    extends ResWheelPosition("10000-Z")
  case object R10000_Y    extends ResWheelPosition("10000-Y")
  case object R10000_J    extends ResWheelPosition("10000-J")
  case object R10000_H    extends ResWheelPosition("10000-H")
  case object R1000_K     extends ResWheelPosition("1000-K")
  case object Mirror      extends ResWheelPosition("Mirror")
}

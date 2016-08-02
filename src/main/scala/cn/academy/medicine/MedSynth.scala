package cn.academy.medicine

import cn.academy.core.LocalHelper
import cn.academy.medicine.MedSynth.MedicineApplyInfo
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumChatFormatting

/**
  * Handles medicine synthesization logic.
  */
object MedSynth {

  case class MedicineApplyInfo()

}

object Properties {

  trait Property {
    def stackDisplayHint: String
    def internalID: String
    final def displayDesc: String = localProps.get(internalID)
  }

  trait Target extends Property {

    def apply(player: EntityPlayer, data: MedSynth.MedicineApplyInfo)

    def id: String
    override def stackDisplayHint = formatItemDesc("targ", EnumChatFormatting.GREEN, displayDesc)
    override def internalID = "targ_" + id

  }

  trait Strength extends Property {

    def id: String
    override def stackDisplayHint = formatItemDesc("str", EnumChatFormatting.RED, displayDesc)
    override def internalID = "str_" + id

  }

  trait ApplyMethod extends Property {

    def id: String
    override def stackDisplayHint = formatItemDesc("app", EnumChatFormatting.AQUA, displayDesc)
    override def internalID = "app_" + id

  }

  // --- impls

  val Targ_Life = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = ???

    def id = "life"
  }

  val Targ_CP = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = ???

    def id = "cp"
  }

  val Targ_Overload = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = ???

    def id = "overload"
  }

  val Targ_Jump = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = ???

    def id: String = "jump"
  }

  val Targ_Cooldown = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = ???

    override def id: String = "cooldown"
  }

  val Targ_MoveSpeed = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = ???

    override def id: String = "move_speed"
  }


  val Str_Mild = new Strength {
    override def id: String = "mild"
  }

  val Str_Weak = new Strength {
    override def id: String = "weak"
  }

  val Str_Normal = new Strength {
    override def id: String = "normal"
  }

  val Str_Strong = new Strength {
    override def id: String = "strong"
  }




  val Apply_Instant_Incr = new ApplyMethod {
    override def id: String = "instant_incr"
  }

  val Apply_Instant_Decr = new ApplyMethod {
    override def id: String = "instant_decr"
  }

  val Apply_Continuous_Incr = new ApplyMethod {
    override def id: String = "cont_incr"
  }

  val Apply_Continuous_Decr = new ApplyMethod {
    override def id: String = "cont_decr"
  }


  // Misc

  private val local = LocalHelper.at("ac.medicine")
  private val localTypes = local.subPath("prop_type")
  private val localProps = local.subPath("props")

  private def formatItemDesc(propType: String, color: EnumChatFormatting, name: String) = {
    color + localTypes.get(propType) + ": " + EnumChatFormatting.RESET + name
  }

  // --- storage & s11n

  /*
  private val allTargets = Vector(Targ_Life, Targ_CP, Targ_Overload, Targ_Jump)
  private val allStrengths = Vector(Str_Mild, Str_Weak, Str_Normal, Str_Strong)
  private val allMethods = Vector(Apply_Instant_Add, Apply_Instant_Decr, Apply_Continuous_Add, Apply_Continuous_Add)

  val allProperties: Seq[Property] = allTargets ++ allStrengths ++ allMethods

  /**
    * Writes a property into an integer. The number will take up to 8 bits.
    */
  def writeProperty(t: Property): Int = {
    val (typeTag, content) = t match {
      case x: Target => (0, writeTarget(x))
      case x: Strength => (1, writeStrength(x))
      case x: ApplyMethod => (2, writeMethod(x))
      case _ => throw new IllegalArgumentException("Invalid property of type " + t.getClass)
    }

    typeTag & (content << 4)
  }

  /**
    * Reads a property from an integer.
    */
  def readProperty(i: Int): Property = {
    val (typeTag, content) = (i & 0xF, (i >> 4) & 0xF)
    typeTag match {
      case 0 => readTarget(content)
      case 1 => readStrength(content)
      case 2 => readMethod(content)
      case _ => throw new IllegalArgumentException("Invalid input " + i)
    }
  }

  def writeTarget(t: Target): Int = serialize(allTargets, t)
  def readTarget(i: Int) = deserialize(allTargets, i)

  def writeStrength(s: Strength): Int = serialize(allStrengths, s)
  def readStrength(i: Int) = deserialize(allStrengths, i)

  def writeMethod(m: ApplyMethod): Int = serialize(allMethods, m)
  def readMethod(i: Int) = deserialize(allMethods, i)

  private def serialize[T](seq: Seq[T], value: T): Int = {
    seq.indexOf(value)
  }

  private def deserialize[T](seq: Seq[T], idx: Int): T = seq(idx)*/

}

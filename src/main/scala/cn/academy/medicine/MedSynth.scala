package cn.academy.medicine

import cn.academy.core.LocalHelper
import cn.academy.medicine.MedSynth.MedicineApplyInfo
import cn.lambdalib.util.generic.RandUtils
import cn.lambdalib.util.mc.StackUtils
import com.google.common.base.Preconditions
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.{DamageSource, EnumChatFormatting}

/**
  * Handles medicine synthesizing logic.
  */
object MedSynth {

  case class MedicineApplyInfo(target: Properties.Target,
                               strengthType: Properties.Strength,
                               strengthModifier: Float,
                               method: Properties.ApplyMethod)

  def writeApplyInfo(stack: ItemStack, info: MedicineApplyInfo): Unit = {
    val tag0 = StackUtils.loadTag(stack)
    val tag = new NBTTagCompound
    tag0.setTag("medicine", tag)

    tag.setInteger("target", Properties.writeTarget(info.target))
    tag.setInteger("strengthType", Properties.writeStrength(info.strengthType))
    tag.setFloat("strengthMod", info.strengthModifier)
    tag.setInteger("method", Properties.writeMethod(info.method))
  }

  def readApplyInfo(stack: ItemStack): MedicineApplyInfo = {
    def error() = throw new IllegalArgumentException("Invalid stack tag to read medicine info")

    Preconditions.checkNotNull(StackUtils.loadTag(stack)).getTag("medicine") match {
      case tag: NBTTagCompound =>
        val target = Properties.readTarget(tag.getInteger("target"))
        val strengthType = Properties.readStrength(tag.getInteger("strengthType"))
        val strengthMod = tag.getFloat("strengthMod")
        val method = Properties.readMethod(tag.getInteger("method"))

        MedicineApplyInfo(target, strengthType, strengthMod, method)
      case _ => error()
    }
  }

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
    val instant: Boolean
    val incr: Boolean

    def id: String
    override def stackDisplayHint = formatItemDesc("app", EnumChatFormatting.AQUA, displayDesc)
    override def internalID = "app_" + id

  }

  // --- impls

  val Targ_Life = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {
      if (data.method.instant) {
        if (data.method.incr) {
          val amt = 10 * data.strengthModifier
          player.heal(amt)
        } else {
          val amt = 5 * data.strengthModifier
          player.attackEntityFrom(DamageSource.causePlayerDamage(player), amt)
        }
      } else { // Continuous recovery
        if (data.method.incr) {
          ???
        } else {
          ???
        }
      }
    }

    def id = "life"
  }

  val Targ_CP = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {

    }

    def id = "cp"
  }

  val Targ_Overload = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {

    }

    def id = "overload"
  }

  val Targ_Jump = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {

    }

    def id: String = "jump"
  }

  val Targ_Cooldown = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {

    }

    override def id: String = "cooldown"
  }

  val Targ_MoveSpeed = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {

    }

    override def id: String = "move_speed"
  }

  val Targ_Disposed = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {
      val test = RandUtils.rangef(0, 1)
      val world = player.worldObj

      test match {
        case p if p < 0.5f => // No effect but adds sensitivity
          println("+Sensitivity")

        case p if p < 0.75f => // Debuff
          println("Debuff")

        case _ => // Fake Explosion
          world.playSoundEffect(player.posX, player.posY, player.posZ, "random.explode", 4.0f, 1.0f)
          player.attackEntityFrom(DamageSource.causePlayerDamage(player), 10f)
      }
    }

    override def id: String = "disposed"
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
    val incr = true
    val instant = true

    override def id: String = "instant_incr"
  }

  val Apply_Instant_Decr = new ApplyMethod {
    val incr = false
    val instant = true

    override def id: String = "instant_decr"
  }

  val Apply_Continuous_Incr = new ApplyMethod {
    val incr = true
    val instant = false

    override def id: String = "cont_incr"
  }

  val Apply_Continuous_Decr = new ApplyMethod {
    val incr = false
    val instant = false

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


  // For cross-version compatibility, only append new properties at the end of lists.

  private val allTargets = Vector(Targ_Life, Targ_CP, Targ_Overload, Targ_Jump, Targ_Disposed)
  private val allStrengths = Vector(Str_Mild, Str_Weak, Str_Normal, Str_Strong)
  private val allMethods = Vector(Apply_Instant_Incr, Apply_Instant_Decr, Apply_Continuous_Decr, Apply_Continuous_Incr)

  val allProperties: Seq[Property] = allTargets ++ allStrengths ++ allMethods

  /*
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
  } */

  def writeTarget(t: Target): Int = serialize(allTargets, t)
  def readTarget(i: Int) = deserialize(allTargets, i)

  def writeStrength(s: Strength): Int = serialize(allStrengths, s)
  def readStrength(i: Int) = deserialize(allStrengths, i)

  def writeMethod(m: ApplyMethod): Int = serialize(allMethods, m)
  def readMethod(i: Int) = deserialize(allMethods, i)

  private def serialize[T](seq: Seq[T], value: T): Int = {
    seq.indexOf(value)
  }

  private def deserialize[T](seq: Seq[T], idx: Int): T = seq(idx)

}

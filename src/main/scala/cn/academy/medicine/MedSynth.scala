package cn.academy.medicine

import cn.academy.core.LocalHelper
import cn.academy.medicine.MedSynth.MedicineApplyInfo
import cn.lambdalib.util.generic.RandUtils
import cn.lambdalib.util.helper.Color
import cn.lambdalib.util.mc.StackUtils
import com.google.common.base.Preconditions
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.{DamageSource, EnumChatFormatting}

/**
  * Handles medicine synthesizing logic.
  *
  * TODO Design a command that generates medicine
  * TODO Debug display for medicine data
  * TODO Fill up all buff effect implementations
  *
  */
object MedSynth {

  case class MedicineApplyInfo(target: Properties.Target,
                               strengthType: Properties.Strength,
                               strengthModifier: Float,
                               method: Properties.ApplyMethod,
                               sensitiveRatio: Float) {
    import org.lwjgl.util.{Color => LColor}

    private implicit def c2l(x: Color):LColor = new LColor(
      (x.r*255).toByte, (x.g*255).toByte,
      (x.b*255).toByte, (x.a*255).toByte)

    private implicit def l2c(x: LColor): Color = new Color(x.getRed/255.0f, x.getGreen/255.0f, x.getBlue/255.0f, x.getAlpha/255.0f)

    private def toHSB(color: Color): (Float, Float, Float) = {
      val lwjColor: LColor = color
      val arr = lwjColor.toHSB(null)
      (arr(0), arr(1), arr(2))
    }

    private def fromHSB(hsb: (Float, Float, Float)): Color = {
      val ret = new LColor()
      hsb match { case (h, s, b) => ret.fromHSB(h, s, b) }
      ret
    }

    lazy val displayColor: Color = {
      import Properties._

      val (h, s, _) = toHSB(target.baseColor)
      val b = math.min(1f, strengthModifier * 0.6666f)

      fromHSB((h, s, b))
    }

  }

  def writeApplyInfo(stack: ItemStack, info: MedicineApplyInfo): Unit = {
    val tag0 = StackUtils.loadTag(stack)
    val tag = new NBTTagCompound
    tag0.setTag("medicine", tag)

    tag.setInteger("target", Properties.writeTarget(info.target))
    tag.setInteger("strengthType", Properties.writeStrength(info.strengthType))
    tag.setFloat("strengthMod", info.strengthModifier)
    tag.setInteger("method", Properties.writeMethod(info.method))
    tag.setFloat("sens", info.sensitiveRatio)
  }

  def readApplyInfo(stack: ItemStack): MedicineApplyInfo = {
    def error() = throw new IllegalArgumentException("Invalid stack tag to read medicine info")

    Preconditions.checkNotNull(StackUtils.loadTag(stack)).getTag("medicine") match {
      case tag: NBTTagCompound =>
        val target = Properties.readTarget(tag.getInteger("target"))
        val strengthType = Properties.readStrength(tag.getInteger("strengthType"))
        val strengthMod = tag.getFloat("strengthMod")
        val method = Properties.readMethod(tag.getInteger("method"))
        val sensitiveRatio = tag.getFloat("sens")

        MedicineApplyInfo(target, strengthType, strengthMod, method, sensitiveRatio)
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

    val baseColor: Color
    val medSensitiveRatio: Float

    def id: String

    override def stackDisplayHint = formatItemDesc("targ", EnumChatFormatting.GREEN, displayDesc)
    override def internalID = "targ_" + id

  }

  trait Strength extends Property {

    val baseValue: Float
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

  trait Variation extends Property {
    def id: String
    override def internalID = "var_" + id
    override def stackDisplayHint = formatItemDesc("var", EnumChatFormatting.DARK_PURPLE, displayDesc)
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
        val buffData = BuffData(player)
        if (data.method.incr) {
          buffData.addBuffInfinite(new BuffHeal(0.25f * data.strengthModifier))
        } else {
          // ???
        }
      }
    }

    def id = "life"
    val baseColor = new Color(0xffff0000)
    val medSensitiveRatio = 0.05f
  }

  val Targ_CP = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {

    }

    def id = "cp"
    val baseColor = new Color(0xff0000ff)
    val medSensitiveRatio = 0.05f
  }

  val Targ_Overload = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {

    }

    def id = "overload"
    val baseColor = new Color(0xffffff00)
    val medSensitiveRatio = 0.05f
  }

  val Targ_Jump = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {

    }

    def id: String = "jump"
    val baseColor = new Color(0xffffffff)
    val medSensitiveRatio = 0.03f
  }

  val Targ_Cooldown = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {

    }

    override def id: String = "cooldown"
    val baseColor = new Color(0xff0000ff)
    val medSensitiveRatio = 0.1f
  }

  val Targ_MoveSpeed = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {

    }

    override def id: String = "move_speed"
    val baseColor = new Color(0xffffffff)
    val medSensitiveRatio = 0.03f
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
    val baseColor = new Color(0xff000000)
    val medSensitiveRatio = 0.5f
  }

  val Targ_Attack = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {
      // TODO
    }

    override def id: String = "attack"

    override val medSensitiveRatio: Float = 0
    override val baseColor: Color = new Color(0xffff00ff)
  }


  val Str_Mild = new Strength {
    val baseValue = 0.3f

    override def id: String = "mild"
  }

  val Str_Weak = new Strength {
    val baseValue = 0.6f

    override def id: String = "weak"
  }

  val Str_Normal = new Strength {
    val baseValue = 0.9f

    override def id: String = "normal"
  }

  val Str_Strong = new Strength {
    val baseValue = 1.5f

    override def id: String = "strong"
  }

  val Str_Infinity = new Strength {
    val baseValue = 10000f
    override def id: String = "infinity"
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



  val Var_Infinity = new Variation {
    override def id = "infinity"
  }

  val Var_Neutralize = new Variation {
    override def id = "neutralize"
  }

  val Var_Desens = new Variation {
    override def id = "desens"
  }

  val Var_Fluct = new Variation {
    override def id: String = "fluct"
  }

  val Var_Stabilize = new Variation {
    override def id: String = "stabilize"
  }

  // Misc

  private val local = LocalHelper.at("ac.medicine")
  private val localTypes = local.subPath("prop_type")
  private val localProps = local.subPath("props")
  private val applyMethodMapping = List(Apply_Instant_Incr, Apply_Instant_Decr, Apply_Continuous_Incr, Apply_Continuous_Decr)
    .map(eff => (eff.instant, eff.incr) -> eff)
    .toMap

  def findApplyMethod(instant: Boolean, incr: Boolean) = applyMethodMapping((instant, incr))

  private def formatItemDesc(propType: String, color: EnumChatFormatting, name: String) = {
    color + localTypes.get(propType) + ": " + EnumChatFormatting.RESET + name
  }

  // --- storage & s11n


  // For cross-version compatibility, only append new properties at the end of lists.

  private val allTargets = Vector(Targ_Life, Targ_CP, Targ_Overload, Targ_Jump, Targ_Disposed, Targ_Attack)
  private val allStrengths = Vector(Str_Mild, Str_Weak, Str_Normal, Str_Strong, Str_Infinity)
  private val allMethods = Vector(Apply_Instant_Incr, Apply_Instant_Decr, Apply_Continuous_Decr, Apply_Continuous_Incr)
  private val allVariations = Vector(Var_Infinity, Var_Neutralize, Var_Desens, Var_Stabilize, Var_Fluct)

  val allProperties: Seq[Property] = allTargets ++ allStrengths ++ allMethods ++ allVariations

  def writeTarget(t: Target): Int = serialize(allTargets, t)
  def readTarget(i: Int) = deserialize(allTargets, i)

  def writeStrength(s: Strength): Int = serialize(allStrengths, s)
  def readStrength(i: Int) = deserialize(allStrengths, i)

  def writeMethod(m: ApplyMethod): Int = serialize(allMethods, m)
  def readMethod(i: Int) = deserialize(allMethods, i)

  def writeVariation(m: Variation): Int = serialize(allVariations, m)
  def readVariation(i: Int) = deserialize(allVariations, i)

  private def serialize[T](seq: Seq[T], value: T): Int = {
    val idx = seq.indexOf(value)
    if (idx == -1) {
      throw new IllegalArgumentException("Can't serialize " + value)
    }
    idx
  }

  private def deserialize[T](seq: Seq[T], idx: Int): T = seq(idx)

}

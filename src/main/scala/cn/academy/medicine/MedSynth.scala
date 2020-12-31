package cn.academy.medicine

//import cn.academy.ability.api.cooldown.CooldownData
//import cn.academy.ability.api.data.CPData
//import cn.academy.core.LocalHelper
import cn.academy.datapart.{CPData, CooldownData}
import cn.academy.medicine.MedSynth.MedicineApplyInfo
import cn.academy.util.LocalHelper
import cn.lambdalib2.util.{Colors, RandUtils, StackUtils}
import net.minecraft.init.{MobEffects, SoundEvents}
import net.minecraft.util.SoundCategory
import net.minecraft.util.text.TextFormatting
import org.lwjgl.util.Color
//import cn.lambdalib.util.generic.RandUtils
//import cn.lambdalib.util.helper.Color
//import cn.lambdalib.util.mc.StackUtils
import com.google.common.base.Preconditions
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.potion.{Potion, PotionEffect}
import net.minecraft.util.{DamageSource}

/**
  * Handles medicine synthesizing logic.
  *
  * TODO Debug display for medicine data
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
      (x.getRed*255).toByte, (x.getGreen*255).toByte,
      (x.getBlue*255).toByte, (x.getAlpha*255).toByte)

    private implicit def l2c(x: LColor): Color = Colors.fromFloat(x.getRed/255.0f, x.getGreen/255.0f, x.getBlue/255.0f, x.getAlpha/255.0f)

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

    override def stackDisplayHint = formatItemDesc("targ", TextFormatting.GREEN, displayDesc)
    override def internalID = "targ_" + id

  }

  trait Strength extends Property {

    val baseValue: Float
    def id: String
    override def stackDisplayHint = formatItemDesc("str", TextFormatting.RED, displayDesc)
    override def internalID = "str_" + id

  }

  trait ApplyMethod extends Property {
    val instant: Boolean
    val incr: Boolean
    val strength: Float

    def id: String
    override def stackDisplayHint = formatItemDesc("app", TextFormatting.AQUA, displayDesc)
    override def internalID = "app_" + id
  }

  trait Variation extends Property {
    def id: String
    override def internalID = "var_" + id
    override def stackDisplayHint = formatItemDesc("var", TextFormatting.DARK_PURPLE, displayDesc)
  }

  // --- impls
  val ContApplyTime = 15 * 20

  val Targ_Life = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {
      val amt = 5 * data.strengthModifier
      if (data.method.instant) {
        if (data.method.incr) {
          player.heal(amt)
        } else {
          player.attackEntityFrom(DamageSource.causePlayerDamage(player), amt)
        }
      } else { // Continuous recovery
        val buffData = BuffData(player)
        val time = ContApplyTime

        val buff = new BuffHeal(amt)

        buffData.addBuff(buff, time)
      }
    }

    def id = "life"
    val baseColor = Colors.fromHexColor(0xffff0000)
    val medSensitiveRatio = 0.05f
  }

  val Targ_CP = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {
      val cpData = CPData.get(player)
      val baseValue = cpData.getMaxCP * 0.1f * data.strengthModifier

      if (data.method.instant) {
        cpData.setCP(cpData.getCP + baseValue)
      } else {
        val buffData = BuffData(player)
        val time = ContApplyTime
        val perTick = baseValue
        buffData.addBuff(new BuffCPRecovery(perTick), time)
      }
    }

    def id = "cp"
    val baseColor = Colors.fromHexColor(0xff0000ff)
    val medSensitiveRatio = 0.05f
  }

  val Targ_Overload = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {
      val cpData = CPData.get(player)
      val amt = cpData.getMaxOverload * 0.1f * data.strengthModifier

      if (data.method.instant) {
        cpData.setOverload(cpData.getOverload - amt)
      } else {
        BuffData(player).addBuff(new BuffOverloadRecovery(amt), ContApplyTime)
      }
    }

    def id = "overload"
    val baseColor = Colors.fromHexColor(0xffffff00)
    val medSensitiveRatio = 0.05f
  }

  val Targ_Jump = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {
      require(data.method == Apply_Continuous_Incr)

      val time = ContApplyTime
      val eff = new PotionEffect(MobEffects.JUMP_BOOST, time, strenghToLevel(data.strengthType))
      player.addPotionEffect(eff)
    }

    def id: String = "jump"
    val baseColor = Colors.fromHexColor(0xffffffff)
    val medSensitiveRatio = 0.03f
  }

  val Targ_Cooldown = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {
      val baseValue = 0.2f * data.strengthModifier
      if (data.method.instant) {
        import scala.collection.JavaConversions._
        for (cd <- CooldownData.of(player).rawData.values) {
          cd.setTickLeft((cd.getTickLeft - baseValue * cd.getMaxTick).toInt)
        }
      } else {
        BuffData(player).addBuff(new BuffCooldownRecovery(baseValue), ContApplyTime)
      }
    }

    override def id: String = "cooldown"
    val baseColor = Colors.fromHexColor(0xff0000ff)
    val medSensitiveRatio = 0.1f
  }

  val Targ_MoveSpeed = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {
      require(!data.method.instant)

      val time = ContApplyTime
      val potion = if (data.method.incr) MobEffects.SPEED else MobEffects.SLOWNESS
      player.addPotionEffect(new PotionEffect(potion, time, strenghToLevel(data.strengthType)))
    }

    override def id: String = "move_speed"
    val baseColor = Colors.fromHexColor(0xffffffff)
    val medSensitiveRatio = 0.03f
  }

  val Targ_Disposed = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {
      val test = RandUtils.rangef(0, 1)
      val world = player.world

      test match {
        case p if p < 0.5f => // No effect but adds sensitivity
          println("+Sensitivity")

        case p if p < 0.75f => // Debuff
          println("Debuff")

          SoundCategory.PLAYERS
        case _ => // Fake Explosion
          player.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 4.0f, 1.0f)
          player.attackEntityFrom(DamageSource.causePlayerDamage(player), 10f)
      }
    }

    override def id: String = "disposed"
    val baseColor = Colors.fromHexColor(0xff000000)
    val medSensitiveRatio = 0.5f
  }

  val Targ_Attack = new Target {
    override def apply(player: EntityPlayer, data: MedicineApplyInfo): Unit = {
      require(!data.method.instant)

      val time = ContApplyTime
      val boostRatio = 1 + (0.2f * data.strengthModifier)

      BuffData(player).addBuff(new BuffAttackBoost(boostRatio, player.getName), time)
    }

    override def id: String = "attack"

    override val medSensitiveRatio: Float = 0
    override val baseColor: Color = Colors.fromHexColor(0xffff00ff)
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
    val strength = 2f

    override def id: String = "instant_incr"
  }

  val Apply_Instant_Decr = new ApplyMethod {
    val incr = false
    val instant = true
    val strength = -1f

    override def id: String = "instant_decr"
  }

  val Apply_Continuous_Incr = new ApplyMethod {
    val incr = true
    val instant = false
    val strength = 0.01f

    override def id: String = "cont_incr"
  }

  val Apply_Continuous_Decr = new ApplyMethod {
    val incr = false
    val instant = false
    val strength = -0.005f

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

  def strenghToLevel(strength: Strength) = strength match {
    case Str_Mild => 0
    case Str_Weak => 1
    case Str_Normal => 2
    case Str_Strong => 3
    case Str_Infinity => 4
  }

  private def formatItemDesc(propType: String, color: TextFormatting, name: String) = {
    color + localTypes.get(propType) + ": " + TextFormatting.RESET + name
  }

  // --- storage & s11n


  // For cross-version compatibility, only append new properties at the end of lists.

  private val allTargets = Vector(Targ_Life, Targ_CP, Targ_Overload, Targ_Jump, Targ_Disposed, Targ_Attack, Targ_Cooldown)
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

  def find(name: String): Option[Property] = allProperties.find(_.internalID == name)

  private def serialize[T](seq: Seq[T], value: T): Int = {
    val idx = seq.indexOf(value)
    if (idx == -1) {
      throw new IllegalArgumentException("Can't serialize " + value)
    }
    idx
  }

  private def deserialize[T](seq: Seq[T], idx: Int): T = seq(idx)

}

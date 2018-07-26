package cn.academy.vanilla.electromaster.skill

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context._
import cn.academy.core.client.ACRenderingHelper
import cn.academy.core.client.sound.{ACSounds, FollowEntitySound}
import cn.academy.support.{EnergyBlockHelper, EnergyItemHelper}
import cn.academy.vanilla.electromaster.client.effect.ArcPatterns
import cn.academy.vanilla.electromaster.entity.EntitySurroundArc.ArcType
import cn.academy.vanilla.electromaster.entity.{EntityArc, EntitySurroundArc}
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.s11n.network.NetworkS11nType
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.SoundCategory
/**
  * @author WeAthFolD, KSkun
  */
object CurrentCharging extends Skill("charging", 1) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = {
    activateSingleKey(rt, keyid, p => new ChargingContext(p))
  }

}

object ChargingBlockContext {

  final val MSG_EFFECT_START = "effect_start"
  final val MSG_EFFECT_END = "effect_end"

}

import ChargingBlockContext._
import cn.academy.ability.api.AbilityAPIExt._

class ChargingContext(p: EntityPlayer) extends Context(p, CurrentCharging) {

  def getChargingSpeed(exp: Float): Float = lerpf(15, 35, exp)
  def getExpIncr(effective: Boolean): Float = if(effective) 0.0001f else 0.00003f
  def getConsumption(exp: Float): Float = lerpf(3, 7, exp)
  def getOverload(exp: Float): Float = lerpf(65, 48, exp)

  private var overload = 0f

  val distance = 15.0d
  private val exp = ctx.getSkillExp
  private val isItem = ctx.player.getHeldEquipment != null

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  private def s_onStart() = {
    ctx.consume(getOverload(exp), 0)
    overload = ctx.cpData.getOverload
  }

  @Listener(channel=MSG_KEYDOWN, side=Array(Side.CLIENT))
  private def l_onStart() = {
    sendToServer(MSG_EFFECT_START, isItem.asInstanceOf[AnyRef])
  }

  @Listener(channel=MSG_EFFECT_START, side=Array(Side.SERVER))
  private def s_onEffectStart(isItem: Boolean) = {
    sendToClient(MSG_EFFECT_START, isItem.asInstanceOf[AnyRef])
  }

  @Listener(channel=MSG_EFFECT_END, side=Array(Side.SERVER))
  private def s_onEffectEnd(isItem: Boolean) = {
    sendToClient(MSG_EFFECT_END, isItem.asInstanceOf[AnyRef])
    terminate()
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_onTick() = {
    if(ctx.cpData.getOverload < overload) ctx.cpData.setOverload(overload)
    if(!isItem) {
      // Perform raytrace
      val pos = Raytrace.traceLiving(player, distance)

      var good = false
      if (pos != null && pos.typeOfHit == MovingObjectType.BLOCK) {
        val tile = player.getEntityWorld.getTileEntity(pos.blockX, pos.blockY, pos.blockZ)
        if (EnergyBlockHelper.isSupported(tile)) {
          good = true

          val charge = getChargingSpeed(exp)
          EnergyBlockHelper.charge(tile, charge, true)
        }
      }

      ctx.addSkillExp(getExpIncr(good))
      if (!ctx.consume(0, getConsumption(exp))) {
        sendToClient(MSG_EFFECT_END, isItem.asInstanceOf[AnyRef])
        terminate()
      }

      val mod: MovingObjectData = new MovingObjectData
      if (pos != null) {
        mod.blockX = pos.blockX
        mod.blockY = pos.blockY
        mod.blockZ = pos.blockZ
        mod.hitVec = pos.hitVec
        mod.isEntity = pos.typeOfHit == MovingObjectType.ENTITY
        if (mod.isEntity)
          mod.entityEyeHeight = pos.entityHit.getEyeHeight
      } else {
        mod.isNull = true
      }
    } else {
      val stack = player.getHeldEquipment
      val cp = getConsumption(exp)

      if(stack != null && ctx.consume(0, cp)) {
        val amt = getChargingSpeed(exp)

        val good = EnergyItemHelper.isSupported(stack)
        if(good)
          EnergyItemHelper.charge(stack, amt, false)

        ctx.addSkillExp(getExpIncr(good))
      } else {
        sendToClient(MSG_EFFECT_END, isItem.asInstanceOf[AnyRef])
        terminate()
      }
    }
  }

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def l_onEnd() = {
    sendToServer(MSG_EFFECT_END, isItem.asInstanceOf[AnyRef])
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  private def l_onAbort() = {
    sendToServer(MSG_EFFECT_END, isItem.asInstanceOf[AnyRef])
  }

}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[ChargingContext])
class ChargingContextC(par: ChargingContext) extends ClientContext(par) {

  var arc: EntityArc = _
  var surround: EntitySurroundArc = _
  var sound: FollowEntitySound = _

  @Listener(channel=MSG_EFFECT_START, side=Array(Side.CLIENT))
  private def c_startEffects(isItem: Boolean) = {
    if(!isItem) {
      arc = new EntityArc(player, ArcPatterns.chargingArc)
      player.getEntityWorld.spawnEntityInWorld(arc)
      arc.lengthFixed = false
      arc.hideWiggle = 0.8
      arc.showWiggle = 0.2
      arc.texWiggle = 0.8

      surround = new EntitySurroundArc(player.getEntityWorld, player.posX, player.posY, player.posZ, 1, 1)
        .setArcType(ArcType.NORMAL).setLife(100000)
      player.getEntityWorld.spawnEntity(surround)

      sound = new FollowEntitySound(player, "em.charge_loop",SoundCategory.AMBIENT).setLoop().setVolume(0.3f)
      ACSounds.playClient(sound)
    } else {
      sound = new FollowEntitySound(player, "em.charge_loop",SoundCategory.AMBIENT).setLoop().setVolume(0.3f)
      ACSounds.playClient(sound)
      surround = new EntitySurroundArc(player)
      surround.setArcType(ArcType.THIN)
      surround.setLife(100000)
      player.getEntityWorld.spawnEntity(surround)
    }
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def c_updateEffects() = {
    // Perform raytrace
    val pos = Raytrace.traceLiving(player, par.distance)

    var good = false
    if (pos != null && pos.typeOfHit == MovingObjectType.BLOCK) {
      val tile = player.getEntityWorld.getTileEntity(pos.blockX, pos.blockY, pos.blockZ)
      if (EnergyBlockHelper.isSupported(tile)) {
        good = true
      }
    }

    val mod: MovingObjectData = new MovingObjectData
    if (pos != null) {
      mod.blockX = pos.blockX
      mod.blockY = pos.blockY
      mod.blockZ = pos.blockZ
      mod.hitVec = pos.hitVec
      mod.isEntity = pos.typeOfHit == MovingObjectType.ENTITY
      if (mod.isEntity)
        mod.entityEyeHeight = pos.entityHit.getEyeHeight
    } else {
      mod.isNull = true
    }

    var x, y, z = 0d
    if (!mod.isNull) {
      x = mod.hitVec.xCoord
      y = mod.hitVec.yCoord
      z = mod.hitVec.zCoord
      if (mod.isEntity) {
        y += mod.entityEyeHeight
      }
    } else {
      val mo = new Motion3D(player, true).move(par.distance)
      x = mo.px
      y = mo.py
      z = mo.pz
    }
    if(arc != null) arc.setFromTo(player.posX, player.posY + ACRenderingHelper.getHeightFix(player), player.posZ, x, y, z)

    if(surround != null) {
      if(good) {
        surround.updatePos(mod.blockX + 0.5, mod.blockY, mod.blockZ + 0.5)
        surround.draw = true
      } else {
        surround.draw = false
      }
    }
  }

  @Listener(channel=MSG_EFFECT_END, side=Array(Side.CLIENT))
  private def c_endEffects(isItem: Boolean) = {
    if(!isItem) {
      if (surround != null) surround.setDead()
      if (arc != null) arc.setDead()
      if (sound != null) sound.stop()
    } else {
      if (sound != null) sound.stop()
      if (surround != null) surround.setDead()
    }
  }

}

@NetworkS11nType
private class MovingObjectData {
  @SerializeIncluded
  var isNull: Boolean = false
  @SerializeIncluded
  var blockX: Int = 0
  @SerializeIncluded
  var blockY: Int = 0
  @SerializeIncluded
  var blockZ: Int = 0
  @SerializeIncluded
  var isEntity: Boolean = false
  @SerializeIncluded
  var entityEyeHeight : Double = 0d
  @SerializeIncluded
  @SerializeNullable
  var hitVec : Vec3 = _
}
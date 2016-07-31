/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.electromaster.skill

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context._
import cn.academy.ability.api.ctrl.ActionManager
import cn.academy.core.client.ACRenderingHelper
import cn.academy.core.client.sound.{ACSounds, FollowEntitySound}
import cn.academy.support.{EnergyItemHelper, EnergyBlockHelper}
import cn.academy.vanilla.electromaster.client.effect.ArcPatterns
import cn.academy.vanilla.electromaster.entity.EntitySurroundArc.ArcType
import cn.academy.vanilla.electromaster.entity.{EntitySurroundArc, EntityArc}
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.MathUtils._
import cn.lambdalib.util.helper.Motion3D
import cn.lambdalib.util.mc.Raytrace
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util.{MovingObjectPosition, ResourceLocation}

/**
  * @author KSkun
  */
object CurrentCharging extends Skill("charging", 1) {

  def getChargingSpeed(exp: Float): Float = lerpf(10, 30, exp)
  def getExpIncr(effective: Boolean): Float = if(effective) 0.0001f else 0.00003f
  def getConsumption(exp: Float): Float = lerpf(6, 14, exp)
  def getOverload(exp: Float): Float = lerpf(65, 48, exp)

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = {
    if(Minecraft.getMinecraft.thePlayer.getCurrentEquippedItem == null)
      activateSingleKey(rt, keyid, p => new ChargingBlockContext(p))
    else
      activateSingleKey(rt, keyid, p => new ChargingItemContext(p))
  }

}

object ChargingBlockContext {

  final val MSG_EFFECT_UPDATE = "effect_update"

}

import CurrentCharging._
import ChargingBlockContext._
import cn.academy.ability.api.AbilityAPIExt._

class ChargingBlockContext(p: EntityPlayer) extends Context(p, CurrentCharging) {

  private val distance = 15.0d
  private val exp = ctx.getSkillExp

  @Listener(channel=MSG_KEYDOWN, side=Array(Side.SERVER))
  private def s_onStart() = {
    ctx.consume(getOverload(exp), 0)
  }

  @Listener(channel=MSG_KEYTICK, side=Array(Side.SERVER))
  private def s_onTick() = {
    // Perform raytrace 
    val pos = Raytrace.traceLiving(player, distance)

    var good = false
    if(pos != null && pos.typeOfHit == MovingObjectType.BLOCK) {
      val tile = player.worldObj.getTileEntity(pos.blockX, pos.blockY, pos.blockZ)
      if(EnergyBlockHelper.isSupported(tile)) {
        good = true

        if(!isRemote) {
          val charge = getChargingSpeed(exp)
          EnergyBlockHelper.charge(tile, charge, true)
        }
      }
    }

    ctx.addSkillExp(getExpIncr(good))
    if (!ctx.consume(0, getConsumption(exp))) {
      terminate()
    }

    sendToClient(MSG_EFFECT_UPDATE, pos, good.asInstanceOf[AnyRef], distance.asInstanceOf[AnyRef])
  }

  @Listener(channel=MSG_KEYUP, side=Array(Side.SERVER))
  private def s_onEnd() = {
    terminate()
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.SERVER))
  private def s_onAbort() = {
    terminate()
  }

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[ChargingBlockContext])
class ChargingBlockContextC(par: ChargingBlockContext) extends ClientContext(par) {

  var arc: EntityArc = _
  var surround: EntitySurroundArc = _
  var sound: FollowEntitySound = _

  @Listener(channel=MSG_KEYDOWN, side=Array(Side.CLIENT))
  private def c_startEffects() = {
    arc = new EntityArc(player, ArcPatterns.chargingArc)
    player.worldObj.spawnEntityInWorld(arc)
    arc.lengthFixed = false
    arc.hideWiggle = 0.8
    arc.showWiggle = 0.2
    arc.texWiggle = 0.8

    surround = new EntitySurroundArc(player.worldObj, player.posX, player.posY, player.posZ, 1, 1)
      .setArcType(ArcType.NORMAL)
    player.worldObj.spawnEntityInWorld(surround)

    sound = new FollowEntitySound(player, "em.charge_loop").setLoop()
    ACSounds.playClient(sound)
  }

  @Listener(channel=MSG_EFFECT_UPDATE, side=Array(Side.CLIENT))
  private def c_updateEffects(res: MovingObjectPosition, isGood: Boolean, distance: Double) = {
    var x, y, z = 0d
    if(res != null) {
      x = res.hitVec.xCoord
      y = res.hitVec.yCoord
      z = res.hitVec.zCoord
      if(res.typeOfHit == MovingObjectType.ENTITY) {
        y += res.entityHit.getEyeHeight
      }
    } else {
      val mo = new Motion3D(player, true).move(distance)
      x = mo.px
      y = mo.py
      z = mo.pz
    }
    arc.setFromTo(player.posX, player.posY + ACRenderingHelper.getHeightFix(player), player.posZ, x, y, z)

    if(isGood) {
      surround.updatePos(res.blockX + 0.5, res.blockY, res.blockZ + 0.5)
      surround.draw = true
    } else {
      surround.draw = false
    }
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def c_endEffects() = {
    if(surround != null) surround.setDead()
    if(arc != null) arc.setDead()
    if(sound != null) sound.stop()
  }

}

object ChargingItemContext {

}

class ChargingItemContext(p: EntityPlayer) extends Context(p, CurrentCharging) {

  private val exp = ctx.getSkillExp

  @Listener(channel=MSG_KEYTICK, side=Array(Side.SERVER))
  private def s_onTick = {
    val stack = player.getCurrentEquippedItem
    val cp = getConsumption(exp)

    if(stack != null && ctx.consume(0, cp)) {
      val amt = getChargingSpeed(exp)

      val good = EnergyItemHelper.isSupported(stack)
      if(good)
        EnergyItemHelper.charge(stack, amt, false)

      ctx.addSkillExp(getExpIncr(good))
    } else {
      terminate()
    }
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.SERVER))
  private def s_onAbort = terminate()

  @Listener(channel=MSG_KEYUP, side=Array(Side.SERVER))
  private def s_onEnd = terminate()

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[ChargingItemContext])
class ChargingIteamContextC(par: ChargingBlockContext) extends ClientContext(par) {

  var sound: FollowEntitySound = _
  var surround: EntitySurroundArc = _

  @Listener(channel=MSG_KEYDOWN, side=Array(Side.CLIENT))
  private def c_startEffects = {
    sound = new FollowEntitySound(player, "em.charge_loop").setLoop()
    ACSounds.playClient(sound)
    surround = new EntitySurroundArc(player)
    surround.setArcType(ArcType.THIN)
    player.worldObj.spawnEntityInWorld(surround)
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def c_endEffects = {
    sound.stop()
    surround.setDead()
  }

}
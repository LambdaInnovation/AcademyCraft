/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.electromaster.skill

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{RegClientContext, ClientContext, Context, ClientRuntime}
import cn.academy.ability.api.data.AbilityData
import cn.academy.core.client.ACRenderingHelper
import cn.academy.core.client.sound.{ACSounds, FollowEntitySound}
import cn.academy.vanilla.electromaster.CatElectromaster
import cn.academy.vanilla.electromaster.client.effect.ArcPatterns
import cn.academy.vanilla.electromaster.entity.EntityArc
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.MathUtils
import cn.lambdalib.util.mc.Raytrace
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.{Vec3, MovingObjectPosition}
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.world.World

/**
  * @author WeAthFolD, KSkun
  */
object MagMovement extends Skill("mag_movement", 2) {

  final val ACCEL = 0.08d

  final val SOUND = "em.move_loop"

  def getMaxDistance(data: AbilityData) = 25

  def toTarget(aData: AbilityData, world: World, pos: MovingObjectPosition): Target = {
    if(pos.typeOfHit == MovingObjectType.BLOCK) {
      val block = world.getBlock(pos.blockX, pos.blockY, pos.blockZ)
      if(aData.getSkillExp(this) < 0.6f && !CatElectromaster.isMetalBlock(block)) { return null }
      if(!CatElectromaster.isWeakMetalBlock(block) && !CatElectromaster.isMetalBlock(block)) { return null }
      new PointTarget(pos.hitVec.xCoord, pos.hitVec.yCoord, pos.hitVec.zCoord)
    } else {
      if(CatElectromaster.isEntityMetallic(pos.entityHit)) {
        return new EntityTarget(pos.entityHit)
      }
      null
    }
  }

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new MovementContext(p))

}

object MovementContext {

  final val MSG_EFFECT_START = "effect_start"
  final val MSG_EFFECT_UPDATE = "effect_update"

}

import MagMovement._
import cn.lambdalib.util.generic.MathUtils._
import cn.academy.ability.api.AbilityAPIExt._
import MovementContext._

class MovementContext(p: EntityPlayer) extends Context(p, MagMovement) {

  private var canSpawnEffect = false

  private var mox, moy, moz: Double = 0d
  private val sx = player.posX
  private val sy = player.posY
  private val sz= player.posZ
  private var target: Target = _

  private val exp = ctx.getSkillExp
  private val cp = lerpf(15, 10, exp)
  private val overload = lerpf(3, 2, exp)

  private val velocity = 1d
  private def getExpIncr(distance: Double) = Math.max(0.005f, 0.0011f * distance.asInstanceOf[Float])
  private def tryAdjust(from: Double, to: Double): Double = {
    val d = to - from
    if(Math.abs(d) < ACCEL) return to
    if(d > 0) from + ACCEL else from - ACCEL
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER, Side.CLIENT))
  private def g_onStart() = {
    val aData = AbilityData.get(player)
    val result = Raytrace.traceLiving(player, getMaxDistance(aData))
    if(result != null) {
      target = toTarget(aData, player.worldObj, result)
      if(target == null) {
        terminate()
      } else {
        canSpawnEffect = true
      }
    } else {
      terminate()
    }
  }

  @Listener(channel=MSG_EFFECT_START, side=Array(Side.SERVER))
  private def s_onEffectStart() = {
    sendToClient(MSG_EFFECT_START)
  }

  @Listener(channel=MSG_EFFECT_UPDATE, side=Array(Side.SERVER))
  private def s_onEffectStart(vec3: Vec3) = {
    sendToClient(MSG_EFFECT_UPDATE, vec3)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def c_onTick() = {
    if(canSpawnEffect) {
      sendToServer(MSG_EFFECT_START)
      canSpawnEffect = false
    }
    if(target != null) {
      target.tick()
      sendToServer(MSG_EFFECT_UPDATE, Vec3.createVectorHelper(target.x, target.y, target.z))
      var dx = target.x - player.posX
      var dy = target.y - player.posY
      var dz = target.z - player.posZ

      val lastMo = MathUtils.lengthSq(player.motionX, player.motionY, player.motionZ)
      if (Math.abs(MathUtils.lengthSq(mox, moy, moz) - lastMo) > 0.5) {
        mox = player.motionX
        moy = player.motionY
        moz = player.motionZ
      }

      val mod = Math.sqrt(dx * dx + dy * dy + dz * dz) / velocity

      dx /= mod
      dy /= mod
      dz /= mod

      player.motionX = tryAdjust(mox, dx)
      mox = tryAdjust(mox, dx)
      player.motionY = tryAdjust(moy, dy)
      moy = tryAdjust(moy, dy)
      player.motionZ = tryAdjust(moz, dz)
      moz = tryAdjust(moz, dz)
    }
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_onTick() = {
    if((target != null && !target.alive()) || !ctx.consume(overload, cp)) terminate()
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.SERVER))
  private def s_onEnd() = {
    val traveledDistance = MathUtils.distance(sx, sy, sz, player.posX, player.posY, player.posZ)
    ctx.addSkillExp(getExpIncr(traveledDistance))
    MagMovement.triggerAchievement(player)

    player.fallDistance = 0.0f
  }

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def l_onEnd() = {
    terminate()
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  private def l_onAbort() = {
    terminate()
  }

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[MovementContext])
class MovementContextC(par: MovementContext) extends ClientContext(par) {

  private var arc: EntityArc = _
  private var sound: FollowEntitySound = _

  @Listener(channel=MSG_EFFECT_START, side=Array(Side.CLIENT))
  private def c_startEffect() = {
    arc = new EntityArc(player, ArcPatterns.thinContiniousArc)
    arc.lengthFixed = false
    arc.texWiggle = 1
    arc.showWiggle = 0.1
    arc.hideWiggle = 0.6

    player.worldObj.spawnEntityInWorld(arc)

    sound = new FollowEntitySound(player, SOUND).setLoop()
    ACSounds.playClient(sound)
  }

  @Listener(channel=MSG_EFFECT_UPDATE, side=Array(Side.CLIENT))
  private def c_updateEffect(target: Vec3) = {
    arc.setFromTo(player.posX, player.posY + ACRenderingHelper.getHeightFix(player), player.posZ, target.xCoord,
      target.yCoord, target.zCoord)
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def c_endEffect() = {
    if(arc != null) arc.setDead()
    if(sound != null) sound.stop()
  }

}

abstract class Target {
  var x, y, z: Double = 0d

  def tick()

  def alive(): Boolean
}

private class PointTarget(_x: Double, _y: Double, _z: Double) extends Target {

  x = _x
  y = _y
  z = _z

  override def tick() = {}

  override def alive() = true

}

private class EntityTarget(_t: Entity) extends Target {

  final val target = _t

  override def tick() = {
    x = target.posX
    y = target.posY + target.getEyeHeight
    z = target.posZ
  }

  override def alive() = !target.isDead

}

private class DummyTarget(_x: Double, _y: Double, _z: Double) extends Target {

  x = _x
  y = _y
  z = _z

  override def tick() = {}

  override def alive() = true

}

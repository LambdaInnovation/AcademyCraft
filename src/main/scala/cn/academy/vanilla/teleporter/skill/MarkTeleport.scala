/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.teleporter.skill

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.ability.api.data.CPData
import cn.academy.core.client.sound.ACSounds
import cn.academy.vanilla.teleporter.entity.EntityTPMarking
import cn.academy.vanilla.teleporter.util.TPSkillHelper
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.VecUtils
import cn.lambdalib.util.helper.Motion3D
import cn.lambdalib.util.mc.Raytrace
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util.{MovingObjectPosition, Vec3}

/**
  * @author WeAthFolD, KSkun
  */
object MarkTeleport extends Skill("mark_teleport", 2) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyID: Int) = activateSingleKey(rt, keyID, p => new MTContext(p))

}

object MTContext {

  final val MSG_EXECUTE = "execute"
  final val MSG_SOUND = "sound"

}

import cn.lambdalib.util.generic.MathUtils._
import cn.academy.ability.api.AbilityAPIExt._
import MTContext._

class MTContext(p: EntityPlayer) extends Context(p, MarkTeleport) {

  private val MINIMUM_VALID_DISTANCE: Double = 3.0

  private var ticks: Int = 0
  private val exp: Float = ctx.getSkillExp

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def l_onKeyUp = sendToServer(MSG_EXECUTE)

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  private def l_onKeyAbort = terminate()

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_tick() = {
    ticks += 1
  }

  @Listener(channel=MSG_EXECUTE, side=Array(Side.SERVER))
  private def s_execute() = {
    val dest: Vec3 = getDest(player, ticks)
    val distance: Float = dest.distanceTo(VecUtils.vec(player.posX, player.posY, player.posZ)).toFloat
    if(distance < MINIMUM_VALID_DISTANCE) {
      // TODO: Play abort sound
    } else {
      sendToClient(MSG_SOUND)
      val overload: Float = lerpf(40, 20, exp)
      ctx.consumeWithForce(overload, distance * getCPB(exp))
      player.setPositionAndUpdate(dest.xCoord, dest.yCoord, dest.zCoord)
      val expincr: Float = 0.00018f * distance
      ctx.addSkillExp(expincr)
      player.fallDistance = 0
      ctx.setCooldown(lerpf(50, 20, exp).toInt)
      TPSkillHelper.incrTPCount(player)
    }
    terminate()
  }

  def getMaxDist(exp: Float, cp: Float, ticks: Int): Double = {
    val max: Double = lerpf(25, 60, exp)
    val cplim: Double = cp / getCPB(exp)
    Math.min((ticks + 1) * 2, Math.min(max, cplim))
  }

  /**
    * @return Consumption per block
    */
  def getCPB(exp: Float): Float = lerpf(13, 5, exp)

  def getDest(player: EntityPlayer, ticks: Int): Vec3 = {
    val cpData: CPData = CPData.get(player)
    val dist: Double = getMaxDist(ctx.getSkillExp, cpData.getCP, ticks)
    val mop: MovingObjectPosition = Raytrace.traceLiving(player, dist)
    var x: Double = .0
    var y: Double = .0
    var z: Double = .0
    if(mop != null) {
      x = mop.hitVec.xCoord
      y = mop.hitVec.yCoord
      z = mop.hitVec.zCoord
      if(mop.typeOfHit == MovingObjectType.BLOCK) {
        mop.sideHit match {
          case 0 =>
            y -= 1.0
          case 1 =>
            y += 1.8
          case 2 =>
            z -= .6
            y = mop.blockY + 1.7
          case 3 =>
            z += .6
            y = mop.blockY + 1.7
          case 4 =>
            x -= .6
            y = mop.blockY + 1.7
          case 5 =>
            x += .6
            y = mop.blockY + 1.7
        }
        // check head
        if(mop.sideHit > 1) {
          val hx: Int = x.toInt
          val hy: Int = (y + 1).toInt
          val hz: Int = z.toInt
          if(!player.worldObj.isAirBlock(hx, hy, hz)) y -= 1.25
        }
      } else y += mop.entityHit.getEyeHeight
    } else {
      val mo: Motion3D = new Motion3D(player, true).move(dist)
      x = mo.px
      y = mo.py
      z = mo.pz
    }
    VecUtils.vec(x, y, z)
  }

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[MTContext])
class MTContextC(par: MTContext) extends ClientContext(par) {

  private var mark: EntityTPMarking = _
  private var ticks = 0

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def l_start() = {
    if(isLocal) {
      mark = new EntityTPMarking(player)
      player.worldObj.spawnEntityInWorld(mark)
    }
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def l_update() = {
    if(mark == null) terminate()

    ticks += 1
    val dest = par.getDest(player, ticks)
    if(isLocal) mark.setPosition(dest.xCoord, dest.yCoord, dest.zCoord)
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def l_end() = {
    if(isLocal) mark.setDead()
  }

  @Listener(channel=MSG_SOUND, side=Array(Side.CLIENT))
  private def c_sound() = {
    ACSounds.playClient(player, "tp.tp", .5f)
  }

}
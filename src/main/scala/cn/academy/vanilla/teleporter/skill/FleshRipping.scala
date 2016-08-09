/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.teleporter.skill

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.core.client.ACRenderingHelper
import cn.academy.core.client.sound.ACSounds
import cn.academy.vanilla.generic.entity.EntityBloodSplash
import cn.academy.vanilla.teleporter.entity.EntityMarker
import cn.academy.vanilla.teleporter.util.TPSkillHelper
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.s11n.{SerializeIncluded, SerializeNullable}
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.s11n.network.NetworkS11n.NetworkS11nType
import cn.lambdalib.util.generic.{RandUtils, VecUtils}
import cn.lambdalib.util.helper.{Color, Motion3D}
import cn.lambdalib.util.mc.{EntitySelectors, Raytrace}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.potion.{Potion, PotionEffect}
import net.minecraft.util.{MovingObjectPosition, Vec3}

/**
  * @author WeAthFolD, KSkun
  */
object FleshRipping extends Skill("flesh_ripping", 3) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyID: Int) = activateSingleKey(rt, keyID, p => new FRContext(p))

}

object FRContext {

  final val MSG_ABORT = "abort"
  final val MSG_END = "end"
  final val MSG_EFFECT_END = "effect_end"

}

import cn.academy.ability.api.AbilityAPIExt._
import cn.lambdalib.util.generic.MathUtils._
import FRContext._

class FRContext(p: EntityPlayer) extends Context(p, FleshRipping) {

  private val exp = ctx.getSkillExp

  private var target: AttackTarget = _

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_tick() = {
    if(!ctx.canConsumeCP(getConsumption)) terminate()
    target = getAttackTarget
  }

  @Listener(channel=MSG_END, side=Array(Side.SERVER))
  private def s_end() = {
    sendToClient(MSG_EFFECT_END, target)
    if(target.target == null) sendToSelf(MSG_ABORT)
    else {
      ctx.consumeWithForce(getOverload, getConsumption)
      TPSkillHelper.attack(ctx, target.target, getDamage)
      if(RandUtils.ranged(0, 1) < getDisgustProb) player.addPotionEffect(new PotionEffect(Potion.confusion.id, 100))
      ctx.setCooldown(lerpf(100, 60, exp).toInt)
      ctx.addSkillExp(.005f)
    }
    terminate()
  }

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def l_onKeyUp = sendToServer(MSG_END)

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  private def l_onKeyAbort = {
    sendToSelf(MSG_EFFECT_END, new AttackTarget())
    terminate()
  }

  @Listener(channel=MSG_ABORT, side=Array(Side.SERVER))
  private def s_abort = target = null

  private def getDamage: Float = lerpf(9, 15, exp)

  override def getRange: Double = lerpf(6, 14, exp)

  private def getDisgustProb: Float = .05f

  private def getConsumption: Float = lerpf(247, 302, exp)

  private def getOverload: Float = lerpf(60, 50, exp)

  def getAttackTarget: AttackTarget = {
    val range: Double = getRange
    val trace: MovingObjectPosition = Raytrace.traceLiving(player, range, EntitySelectors.living)
    var target: Entity = null
    var dest: Vec3 = null
    if (trace != null) {
      target = trace.entityHit
      dest = trace.hitVec
    }
    else dest = new Motion3D(player, true).move(range).getPosVec
    new AttackTarget(dest, target, player)
  }

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[FRContext])
class FRContextC(par: FRContext) extends ClientContext(par) {

  private var marker: EntityMarker = _

  private val DISABLED_COLOR: Color = new Color().setColor4i(74, 74, 74, 160)
  private val THREATENING_COLOR: Color = new Color().setColor4i(185, 25, 25, 180)

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def l_terminated() = {
    if(marker != null) marker.setDead()
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def l_startEffect() = {
    if(isLocal) {
      marker = new EntityMarker(player.worldObj)
      marker.setPosition(player.posX, player.posY, player.posZ)
      marker.color = DISABLED_COLOR
      player.worldObj.spawnEntityInWorld(marker)
    }
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def l_updateEffect() = {
    if(isLocal) {
      val at: AttackTarget = par.getAttackTarget
      marker.setPosition(at.dest.xCoord, at.dest.yCoord, at.dest.zCoord)
      if (at.target == null) {
        marker.color = DISABLED_COLOR
        marker.width = 1.0f
        marker.height = 1.0f
      }
      else {
        marker.color = THREATENING_COLOR
        marker.width = at.target.width * 1.2f
        marker.height = at.target.height * 1.2f
      }
    }
  }

  @Listener(channel=MSG_EFFECT_END, side=Array(Side.CLIENT))
  private def c_endEffect(target: AttackTarget) = {
    if(isLocal) marker.setDead()

    if(target != null && target.target != null) {
      ACSounds.playClient(player, "tp.guts", 0.6f)
      val e: Entity = target.target
      for(i <- 0 to RandUtils.rangei(4, 6)) {
        var y: Double = e.posY + RandUtils.ranged(0, 1) * e.height
        e match {
          case player1: EntityPlayer => y += ACRenderingHelper.getHeightFix(player1)
          case _ =>
        }
        val theta: Double = RandUtils.ranged(0, Math.PI * 2)
        val r: Double = 0.5 * RandUtils.ranged(0.8 * e.width, e.width)
        val splash: EntityBloodSplash = new EntityBloodSplash(player.worldObj)
        splash.setPosition(e.posX + r * Math.sin(theta), y, e.posZ + r * Math.cos(theta))
        player.worldObj.spawnEntityInWorld(splash)
      }
    }
  }

}

@Registrant
@NetworkS11nType
class AttackTarget() {

  @SerializeIncluded
  var dest: Vec3 = _
  @SerializeIncluded
  @SerializeNullable
  var target: Entity = _
  @SerializeIncluded
  var player: EntityPlayer = _

  def this(_dest: Vec3, _target: Entity, _player: EntityPlayer) {
    this()
    dest = _dest
    target = _target
    player = _player
  }

  def this(tag: NBTTagCompound, _player: EntityPlayer) {
    this(VecUtils.vec(tag.getFloat("x"), tag.getFloat("y"), tag.getFloat("z")),
      _player.worldObj.getEntityByID(tag.getInteger("i")), _player)
  }

  private def toNBT: NBTTagCompound = {
    val ret: NBTTagCompound = new NBTTagCompound
    ret.setFloat("x", dest.xCoord.toFloat)
    ret.setFloat("y", dest.yCoord.toFloat)
    ret.setFloat("z", dest.zCoord.toFloat)
    ret.setInteger("i", if(target == null) 0 else target.getEntityId)
    ret
  }

}

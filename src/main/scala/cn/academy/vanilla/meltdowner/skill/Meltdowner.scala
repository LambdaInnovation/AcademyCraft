/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.meltdowner.skill

import java.util.function.Consumer

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.core.client.ACRenderingHelper
import cn.academy.core.client.sound.ACSounds
import cn.academy.core.client.sound.FollowEntitySound
import cn.academy.core.util.RangedRayDamage
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory
import cn.academy.vanilla.meltdowner.entity.EntityMDRay
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.MathUtils
import cn.lambdalib.util.generic.VecUtils
import cn.lambdalib.util.helper.Motion3D
import cn.lambdalib.util.mc.Raytrace
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util.Vec3
import cn.lambdalib.util.generic.MathUtils.lerpf
import cn.lambdalib.util.generic.RandUtils.ranged
import cn.lambdalib.util.generic.RandUtils.rangei

/**
  * @author WeAthFolD
  */
object Meltdowner extends Skill("meltdowner", 3) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = {
    activateSingleKey(rt, keyid, p => new MDContext(p))
  }

}

object MDContext {

  final val MSG_PERFORM = "perform"
  final val MSG_REFLECTED = "reflect"
  final val TICKS_MIN: Int = 20
  final val TICKS_MAX: Int = 40
  final val TICKS_TOLE: Int = 100

}

import cn.academy.ability.api.AbilityAPIExt._
import MDContext._

class MDContext(player: EntityPlayer) extends Context(player, Meltdowner) {

  private var ticks: Int = 0
  final private val exp: Float = ctx.getSkillExp
  final private val tickConsumption: Float = lerpf(10, 15, exp)

  private var overloadKeep = 0f

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  private def s_madeAlive() = {
    val overload = lerpf(200, 170, exp)
    ctx.consume(overload, 0)
    overloadKeep = ctx.cpData.getOverload
  }

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def l_keyUp() {
    if(ticks >= MDContext.TICKS_MIN) sendToServer(MSG_PERFORM)
    else terminate()
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  private def l_keyAbort() {
    terminate()
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT, Side.SERVER))
  private def g_tick() {
    ticks += 1
    if(!isRemote) {
      if(ctx.cpData.getOverload < overloadKeep) ctx.cpData.setOverload(overloadKeep)
      if(!ctx.consume(0, tickConsumption) || ticks > MDContext.TICKS_TOLE) terminate()
    }
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  private def s_perform() {
    val ct: Int = toChargeTicks
    val length: Array[Double] = Array[Double](30) // for lambda mod
    val rrd: RangedRayDamage = new RangedRayDamage.Reflectible(ctx, lerpf(2, 3, exp), getEnergy(ct), new Consumer[Entity] {
      override def accept(reflector: Entity): Unit = {
        length.update(0, Math.min(length.apply(0), reflector.getDistanceToEntity(ctx.player)))

        s_reflected(reflector)
        sendToClient(MSG_REFLECTED, reflector)
      }
    })
    rrd.startDamage = getDamage(ct)
    rrd.perform()
    ctx.addSkillExp(getExpIncr(ct))
    ctx.setCooldown(getCooldown(ct))
    sendToClient(MSG_PERFORM, ct.asInstanceOf[AnyRef], length.apply(0).asInstanceOf[AnyRef])
    terminate()
  }

  private def s_reflected(reflector: Entity) {
    val result: MovingObjectPosition = Raytrace.traceLiving(reflector, 10)
    if(result != null && (result.typeOfHit eq MovingObjectType.ENTITY)) ctx.attack(result.entityHit, 0.5f * lerpf(20, 50, exp))
  }

  private def timeRate(ct: Int): Float = MathUtils.lerpf(0.8f, 1.2f, (ct - 20.0f) / 20.0f)

  private def getEnergy(ct: Int): Float = timeRate(ct) * MathUtils.lerpf(300, 700, exp)

  private def getDamage(ct: Int): Float = timeRate(ct) * MathUtils.lerpf(18, 50, exp)

  private def getCooldown(ct: Int): Int = (timeRate(ct) * 20 * MathUtils.lerpf(15, 7, exp)).toInt

  private def getExpIncr(ct: Int): Float = timeRate(ct) * 0.002f

  private def toChargeTicks: Int = Math.min(ticks, MDContext.TICKS_MAX)

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[MDContext])
class MDContextC(par: MDContext) extends ClientContext(par) {

  private var ticks: Int = 0

  private var sound: FollowEntitySound = _

  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  private def c_perform(ct: Int, length: Double) {
    val ray: EntityMDRay = new EntityMDRay(ctx.player, length)
    ACSounds.playClient(ctx.player, "md.meltdowner", 0.5f)
    world.spawnEntityInWorld(ray)
  }

  @Listener(channel=MSG_REFLECTED, side=Array(Side.CLIENT))
  private def c_reflected(reflector: Entity) {
    val playerLook: Vec3 = ctx.player.getLookVec.normalize
    val distance: Double = VecUtils.entityHeadPos(ctx.player).distanceTo(VecUtils.entityHeadPos(reflector))
    val spawnPos: Vec3 = VecUtils.add(VecUtils.entityHeadPos(ctx.player), VecUtils.multiply(playerLook, distance))
    val mo: Motion3D = new Motion3D(reflector, true)
    mo.setPosition(spawnPos.xCoord, spawnPos.yCoord, spawnPos.zCoord)
    val ray: EntityMDRay = new EntityMDRay(ctx.player, mo, 10)
    world.spawnEntityInWorld(ray)
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def c_terminate() {
    if(isLocal) ctx.player.capabilities.setPlayerWalkSpeed(0.1f)
    sound.stop()
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def c_start() {
    sound = new FollowEntitySound(ctx.player, "md.md_charge").setVolume(1.0f)
    ACSounds.playClient(sound)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def c_tick() {
    ticks += 1
    if(isLocal) ctx.player.capabilities.setPlayerWalkSpeed(0.1f - ticks * 0.001f)
    // Particles surrounding player
    for(count <- rangei(2, 3) to 0) {
      val r: Double = ranged(0.7, 1)
      val theta: Double = ranged(0, Math.PI * 2)
      val h: Double = ranged(-1.2, 0)
      val pos: Vec3 = VecUtils.add(VecUtils.vec(ctx.player.posX, ctx.player.posY + (if(ACRenderingHelper.isThePlayer(ctx.player)) 0
      else 1.6), ctx.player.posZ), VecUtils.vec(r * Math.sin(theta), h, r * Math.cos(theta)))
      val vel: Vec3 = VecUtils.vec(ranged(-.03, .03), ranged(.01, .05), ranged(-.03, .03))
      world.spawnEntityInWorld(MdParticleFactory.INSTANCE.next(world, pos, vel))
    }
  }

}
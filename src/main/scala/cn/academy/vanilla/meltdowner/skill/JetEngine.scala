/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.meltdowner.skill

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.vanilla.generic.entity.EntityRippleMark
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory
import cn.academy.vanilla.meltdowner.entity.EntityDiamondShield
import cn.lambdalib2.annoreg.core.Registrant
import cn.lambdalib2.particle.Particle
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util.generic.{RandUtils, VecUtils}
import cn.lambdalib2.util.mc.{EntitySelectors, Raytrace}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.{MovingObjectPosition, Vec3}

/**
  * @author WeAthFolD, KSkun
  */
object JetEngine extends Skill("jet_engine", 4) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyID: Int) = activateSingleKey(rt, keyID, p => new JEContext(p))

}

object JEContext {

  final val MSG_TRIGGER = "trigger"
  final val MSG_MARK_END = "mark_end"

}

import cn.academy.ability.api.AbilityAPIExt._
import cn.lambdalib2.util.generic.MathUtils._
import JEContext._

class JEContext(p: EntityPlayer) extends Context(p, JetEngine) {

  private val exp: Float = ctx.getSkillExp
  private val consumption: Float = lerpf(170, 140, exp)
  private val overload: Float = lerpf(60, 50, exp)

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_onTick() = if(!ctx.canConsumeCP(consumption)) terminate()

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def l_onKeyUp() = sendToServer(MSG_MARK_END)

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  private def l_onKeyAbort() = {
    sendToClient(MSG_MARK_END)
    terminate()
  }

  @Listener(channel=MSG_MARK_END, side=Array(Side.SERVER))
  private def s_onEnd() = {
    if(ctx.consume(consumption, overload)) {
      sendToClient(MSG_MARK_END)
      sendToSelf(MSG_TRIGGER, getDest.addVector(0, 1.65, 0))
      ctx.addSkillExp(.004f)
      JetEngine.triggerAchievement(player)
      ctx.setCooldown(lerpf(60, 30, exp).toInt)
    } else {
      sendToClient(MSG_MARK_END)
      terminate()
    }
  }

  private def getDest: Vec3 = Raytrace.getLookingPos(player, 12, EntitySelectors.nothing).getLeft

  //TRIGGER
  private val TIME: Float = 8
  private val LIFETIME: Float = 15
  private var target: Vec3 = _
  private var ticks: Int = 0
  private var isTriggering = false

  private var start: Vec3 = _
  private var velocity: Vec3 = _

  @Listener(channel=MSG_TRIGGER, side=Array(Side.SERVER))
  private def s_triggerStart(_target: Vec3) = {
    target = _target
    isTriggering = true

    sendToClient(MSG_TRIGGER, target)
  }

  @Listener(channel=MSG_TRIGGER, side=Array(Side.CLIENT))
  private def c_triggerStart(_target: Vec3) = {
    if(isLocal) {
      isTriggering = true
      target = _target

      start = VecUtils.vec(player.posX, player.posY, player.posZ)
      velocity = VecUtils.multiply(VecUtils.subtract(target, start), 1.0 / TIME)
    }
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_triggerTick() = {
    if(isTriggering) {
      val pos: MovingObjectPosition = Raytrace.perform(world,
        VecUtils.vec(player.lastTickPosX, player.lastTickPosY, player.lastTickPosZ),
        VecUtils.vec(player.posX, player.posY, player.posZ), EntitySelectors.exclude(player).and(EntitySelectors.living))
      if(player.ridingEntity!=null)player.mountEntity(null);
      if (pos != null && pos.entityHit != null) MDDamageHelper.attack(ctx, pos.entityHit, lerpf(7, 20, exp))
    }
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def c_triggerTick(): Unit = {
    if(isLocal && isTriggering) {
      if (ticks >= LIFETIME)
        terminate()
      ticks += 1
      val pos: Vec3 = VecUtils.lerp(start, target, ticks / TIME)
      player.setPosition(pos.xCoord, pos.yCoord, pos.zCoord)
      player.motionX = velocity.xCoord
      player.motionY = velocity.yCoord
      player.motionZ = velocity.zCoord
      player.fallDistance = 0.0f
    }
  }

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[JEContext])
class JEContextC(par: JEContext) extends ClientContext(par) {

  private val TIME: Float = 8
  
  private var mark: EntityRippleMark = _

  private var target: Vec3 = _
  private val start: Vec3 = VecUtils.vec(player.posX, player.posY, player.posZ)
  
  private var isMarking = false
  private var ticks: Int = 0
  
  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def l_spawnMark() = {
    if(isLocal) {
      isMarking = true
      mark = new EntityRippleMark(world)
      world.spawnEntityInWorld(mark)
      mark.color.setColor4d(0.2, 1.0, 0.2, 0.7)
    }
  }
  
  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def l_updateMark() = {
    if(isLocal && isMarking) {
      val dest: Vec3 = getDest
      mark.setPosition(dest.xCoord, dest.yCoord, dest.zCoord)
    }
  }

  @Listener(channel=MSG_MARK_END, side=Array(Side.CLIENT))
  private def l_endMark() = {
    if(isLocal) {
      isMarking = false
      mark.setDead()
    }
  }
  
  private def getDest: Vec3 = Raytrace.getLookingPos(player, 12, EntitySelectors.nothing).getLeft

  //TRIGGER
  private var entity: EntityDiamondShield = _
  private var isTriggering = false

  @Listener(channel=MSG_TRIGGER, side=Array(Side.CLIENT))
  private def c_tStartEffect(_target: Vec3) = {
    target = _target
    isTriggering = true
    entity = new EntityDiamondShield(player)
    world.spawnEntityInWorld(entity)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def c_tUpdateEffect() = {
    if(isTriggering) {
      ticks += 1
      if (isLocal) player.capabilities.setPlayerWalkSpeed(0.07f)
      for (i <- 0 to 10) {
        val pos2: Vec3 = VecUtils.lerp(start, target, 3 * ticks / TIME)
        val p: Particle = MdParticleFactory.INSTANCE.next(world, VecUtils.add(VecUtils.vec(player.posX, player.posY, player.posZ),
          VecUtils.vec(RandUtils.ranged(-.3, .3), RandUtils.ranged(-.3, .3), RandUtils.ranged(-.3, .3))),
          VecUtils.vec(RandUtils.ranged(-.02, .02), RandUtils.ranged(-.02, .02), RandUtils.ranged(-.02, .02)))
        world.spawnEntityInWorld(p)
      }
    }
  }
  
  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def c_tEndEffect() = {
    if(mark != null) mark.setDead()

    if(isTriggering) {
      if (isLocal) player.capabilities.setPlayerWalkSpeed(0.1f)
      entity.setDead()
    }
    isTriggering = false
  }

}

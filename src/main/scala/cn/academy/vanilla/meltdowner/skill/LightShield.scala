/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.meltdowner.skill

import java.util.function.Predicate

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context._
import cn.academy.core.client.sound.{ACSounds, FollowEntitySound}
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory
import cn.academy.vanilla.meltdowner.entity.EntityMdShield
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.particle.Particle
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.{MathUtils, RandUtils, VecUtils}
import cn.lambdalib.util.helper.Motion3D
import cn.lambdalib.util.mc.{EntitySelectors, WorldUtils}
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.DamageSource
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.living.LivingHurtEvent

/**
  * @author WeAthFolD, KSkun
  */
object LightShield extends Skill("light_shield", 2) {

  MinecraftForge.EVENT_BUS.register(this)

  val ACTION_INTERVAL: Int = 18
  val basicSelector: Predicate[Entity] = EntitySelectors.everything

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyID: Int) = activateSingleKey(rt, keyID, p => new LSContext(p))

  @SubscribeEvent
  def onPlayerAttacked(event: LivingHurtEvent) {
    event.entityLiving match {
      case player: EntityPlayer =>
        val context = ContextManager.instance.find(classOf[LSContext])
        if (context.isPresent) {
          event.ammount = context.get().handleAttacked(event.source, event.ammount)
          if (event.ammount == 0) event.setCanceled(true)
        }
      case _ =>
    }
  }

}

object LSContext {

}

import cn.lambdalib.util.generic.MathUtils._
import cn.academy.ability.api.AbilityAPIExt._
import scala.collection.JavaConversions._
import LightShield._

class LSContext(p: EntityPlayer) extends Context(p, LightShield) {

  private var ticks: Int = 0
  private var lastAbsorb: Int = -1 // The tick last the shield absorbed damage.
  private val exp: Float = ctx.getSkillExp

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def l_onEnd() = {
    terminate()
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  private def l_onAbort() = {
    terminate()
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  private def s_madeAlive() = {
    val overload: Float = lerpf(198, 132, exp)
    if(ctx.consume(overload, 0)) terminate()
  }
  
  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_tick() = {
    ticks += 1

    val cp: Float = lerpf(12, 7, exp)
    if (!ctx.consume(0, cp) && !isRemote) terminate()
    ctx.addSkillExp(1e-6f)

    // Find the entities that are 'colliding' with the shield.
    val candidates: java.util.List[Entity] = WorldUtils.getEntities(player, 3, basicSelector.and(new Predicate[Entity] {
      override def test(t: Entity): Boolean = isEntityReachable(t)
    }).and(EntitySelectors.exclude(player)))
    for (e <- candidates) {
      if (e.hurtResistantTime <= 0 && ctx.consume(getAbsorbOverload, getAbsorbConsumption)) {
        MDDamageHelper.attack(ctx, e, getTouchDamage)
        ctx.addSkillExp(.001f)
      }
    }
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.SERVER))
  private def s_onEnd() = {
    ctx.setCooldown(lerpf(80, 60, exp).toInt)
  }

  def handleAttacked(src: DamageSource, damage: Float): Float = {
    var result = damage
    if (damage == 0 || lastAbsorb != -1 && ticks - lastAbsorb <= ACTION_INTERVAL) return damage
    val entity: Entity = src.getSourceOfDamage
    var perform: Boolean = false
    if (entity != null) if (isEntityReachable(entity)) perform = true
    else perform = true
    if (perform) {
      lastAbsorb = ticks
      if (ctx.consume(getAbsorbConsumption, getAbsorbOverload)) {
        val amt: Float = getAbsorbDamage
        result -= Math.min(damage, amt)
      }
    }
    ctx.addSkillExp(.001f)
    result
  }

  private def getAbsorbDamage: Float = lerpf(15, 50, exp)

  private def getTouchDamage: Float = lerpf(3, 8, exp)

  private def getAbsorbOverload: Float = lerpf(15, 10, exp)

  private def getAbsorbConsumption: Float = lerpf(50, 30, exp)

  private def isEntityReachable(e: Entity): Boolean = {
    val dx: Double = e.posX - player.posX
    //dy = e.posY - player.posY,
    val dz: Double = e.posZ - player.posZ
    val yaw: Double = -MathUtils.toDegrees(Math.atan2(dx, dz))
    Math.abs(yaw - player.rotationYaw) % 360 < 60
  }

}

import RandUtils._

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[LSContext])
class LSContextC(par: LSContext) extends ClientContext(par) {

  private var shield: EntityMdShield = _
  private var loopSound: FollowEntitySound = _

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def c_spawn() = {
    shield = new EntityMdShield(player)
    world.spawnEntityInWorld(shield)
    ACSounds.playClient(player, "md.shield_startup", 0.5f)
    loopSound = new FollowEntitySound(player, "md.shield_loop").setLoop()
    ACSounds.playClient(loopSound)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def c_update() = {
    if (RandUtils.nextFloat < 0.3f) {
      val mo: Motion3D = new Motion3D(player, true).move(1)
      val s: Double = 0.5
      mo.px += ranged(-s, s)
      mo.py += ranged(-s, s)
      mo.pz += ranged(-s, s)
      val p: Particle = MdParticleFactory.INSTANCE.next(world, VecUtils.vec(mo.px, mo.py, mo.pz),
        VecUtils.vec(ranged(-.02, .02), ranged(-.01, .05), ranged(-.02, .02)))
      world.spawnEntityInWorld(p)
    }
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def c_end() = {
    shield.setDead()
    loopSound.stop()
  }

}

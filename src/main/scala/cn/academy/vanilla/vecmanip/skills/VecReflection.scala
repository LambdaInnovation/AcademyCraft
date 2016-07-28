package cn.academy.vanilla.vecmanip.skills

import java.util.function.Predicate

import cn.academy.ability.api.context.ClientRuntime.{ActivateHandlers, IActivateHandler}
import cn.academy.ability.api.{AbilityPipeline, Skill}
import cn.academy.ability.api.context._
import cn.academy.ability.api.ctrl.KeyDelegates
import cn.academy.ability.api.event.ReflectEvent
import cn.academy.core.client.sound.ACSounds
import cn.academy.misc.achievements.ModuleAchievements
import cn.academy.vanilla.vecmanip.client.effect.{WaveEffect, WaveEffectUI}
import cn.academy.vanilla.vecmanip.skills.EntityAffection.{Affected, Excluded}
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.MathUtils._
import cn.lambdalib.util.generic.VecUtils
import cn.lambdalib.util.mc.{Raytrace, Vec3, WorldUtils}
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.command.IEntitySelector
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityXPOrb
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.{EntityLargeFireball, EntityArrow, EntityFireball}
import net.minecraft.util.DamageSource
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.living.{LivingAttackEvent, LivingHurtEvent}

object VecReflection extends Skill("vec_reflection", 4) {

  MinecraftForge.EVENT_BUS.register(this)

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = {
    ModuleAchievements.trigger(rt.getEntity, "vecmanip.vec_reflection")
    rt.addKey(keyid, KeyDelegates.contextActivate(this, new VecReflectionContext(_)))
  }

}

import VecReflectionContext._
import cn.lambdalib.util.generic.RandUtils._
import cn.academy.ability.api.AbilityAPIExt._
import cn.lambdalib.util.mc.MCExtender._
import scala.collection.JavaConversions._
import collection.mutable
import net.minecraft.util.{Vec3 => MCVec3}
import VMSkillHelper._

private object VecReflectionContext {
  final val MSG_EFFECT = "effect"
  final val MSG_REFLECT_ENTITY = "reflect_ent"

  def reflect(entity: Entity, player: EntityPlayer): Unit = {
    val lookPos = Raytrace.getLookingPos(player, 20).getLeft
    val speed = VecUtils.vec(entity.motionX, entity.motionY, entity.motionZ).lengthVector
    val vel = (lookPos - entity.headPosition).normalize * speed
    entity.setVel(vel)
  }
}

class VecReflectionContext(p: EntityPlayer) extends Context(p, VecReflection) {

  private val visited = mutable.Set[Entity]()

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  def s_makeAlive() = {
    MinecraftForge.EVENT_BUS.register(this)
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.SERVER, Side.CLIENT))
  def g_terminate() = {
    MinecraftForge.EVENT_BUS.unregister(this)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  def s_tick() = {
    val range = 4
    val entities = WorldUtils.getEntities(player, range, new Predicate[Entity] {
      override def test(t: Entity): Boolean = true
    })
    entities.removeAll(visited)

    entities.filterNot(EntityAffection.isMarked).foreach (entity => {
      EntityAffection.getAffectInfo(entity) match {
        case Affected(difficulty) =>
          if (!entity.isInstanceOf[EntityXPOrb] && consumeEntity(difficulty)) {
            if(!entity.isInstanceOf[EntityLargeFireball]) {
              reflect(entity, player)
              EntityAffection.mark(entity)
              ctx.addSkillExp(difficulty * 0.0008f)
              sendToClient(MSG_REFLECT_ENTITY, entity)
            } else {
              entity.setDead()
              val sourceEntity = entity.asInstanceOf[EntityLargeFireball].shootingEntity
              val fireball : EntityLargeFireball = new EntityLargeFireball(world(), sourceEntity, sourceEntity.posX,
                sourceEntity.posY, sourceEntity.posZ)
              fireball.setPosition(entity.posX, entity.posY, entity.posZ)
              val lookPos = Raytrace.getLookingPos(player, 20).getLeft
              val speed = VecUtils.vec(entity.motionX, entity.motionY, entity.motionZ).lengthVector
              val vel = (lookPos - entity.headPosition).normalize * speed
              fireball.setVel(vel)
              fireball.field_92057_e = entity.asInstanceOf[EntityLargeFireball].field_92057_e
              EntityAffection.mark(fireball)
              world().spawnEntityInWorld(fireball)
              ctx.addSkillExp(difficulty * 0.0008f)
              sendToClient(MSG_REFLECT_ENTITY, entity)
            }
          }
        case Excluded() =>
      }
    })

    visited ++= entities

    consumeNormal()
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  def c_tick() = {
    consumeNormal()
  }

  @SubscribeEvent
  def onReflect(evt: ReflectEvent) = {
    if (evt.target.equals(player)) {
      evt.setCanceled(true)

      val dpos = evt.player.headPosition - player.headPosition
      sendToClient(MSG_EFFECT, player.position + Vec3(0, ranged(0.4, 1.3), 0) + dpos.normalize() * 0.5)
    }
  }

  /**
    * Note: Canceling the damage event in `LivingHurtEvent` still causes knockback, so there needs
    *  to be one more pre testing.
    */
  @SubscribeEvent
  def onLivingAttack(evt: LivingAttackEvent) = {
    if (evt.entityLiving.equals(player)) {
      val (performed, _) = handleAttack(evt.source, evt.ammount, passby = true)
      if (performed) {
        evt.setCanceled(true)
      }
    }
  }

  @SubscribeEvent
  def onLivingHurt(evt: LivingHurtEvent) = {
    if (evt.entityLiving.equals(player)) {
      val (_, dmg) = handleAttack(evt.source, evt.ammount, passby = false)
      evt.ammount = dmg
    }
  }

  /**
    * @param passby If passby=true, and this isn't a complete absorb, the action will not perform. Else it will.
    * @return (Whether action had been really performed, processed damage)
    */
  private def handleAttack(dmgSource: DamageSource, dmg: Float, passby: Boolean): (Boolean, Float) = {
    val consumpRatio = 110.0f
    val overloadRatio = 15.0f
    val returnRatio = reflectRate

    val consumption = math.min(ctx.cpData.getCP, dmg * consumpRatio)
    val acceptedDamage = consumption / consumpRatio
    val absorbed = acceptedDamage

    val absorbedAll = absorbed == dmg

    if (absorbedAll || !passby) { // Perform the action.
      ctx.consumeWithForce(acceptedDamage * overloadRatio, consumption)
      ctx.addSkillExp(dmg * 0.0004f)

      val sourceEntity = dmgSource.getSourceOfDamage
      if (sourceEntity != null && sourceEntity != player) {
        ctx.attack(sourceEntity, absorbed * returnRatio)
        sendToClient(MSG_EFFECT, sourceEntity.position)
      }

      (true, dmg - absorbed)
    } else {
      (false, dmg - absorbed)
    }
  }

  private val reflectRate = lerpf(0.7f, 1.2f, ctx.getSkillExp) * rangef(0.9f, 1.1f)

  private def consumeEntity(difficulty: Float) = {
    ctx.consume(difficulty * lerpf(30, 16, ctx.getSkillExp), difficulty * lerpf(300, 160, ctx.getSkillExp))
  }

  private def consumeDamage() = ctx.consume(lerpf(30, 10, ctx.getSkillExp), lerpf(300, 200, ctx.getSkillExp))

  private def consumeNormal() = ctx.consume(lerpf(2, 1.5f, ctx.getSkillExp), lerpf(20, 16, ctx.getSkillExp))

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[VecReflectionContext])
class VecReflectionContextC(par: VecReflectionContext) extends ClientContext(par) {

  private var activateHandler: IActivateHandler = _
  private val ui = new WaveEffectUI(0.4f, 110, 1.6f)

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def l_alive() = if (isLocal) {
    activateHandler = ActivateHandlers.terminatesContext(par)
    ClientRuntime.instance.addActivateHandler(activateHandler)
    MinecraftForge.EVENT_BUS.register(this)
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def l_terminate() = if (isLocal) {
    ClientRuntime.instance.removeActiveHandler(activateHandler)
    MinecraftForge.EVENT_BUS.unregister(this)
  }

  @Listener(channel=MSG_REFLECT_ENTITY, side=Array(Side.CLIENT))
  private def c_reflectEntity(ent: Entity) = {
    reflect(ent, player)
    reflectEffect(ent.headPosition)
  }

  @Listener(channel=MSG_EFFECT, side=Array(Side.CLIENT))
  private def reflectEffect(point: MCVec3) = {
    val eff = new WaveEffect(world, 2, 1.1)
    eff.setPos(point)
    eff.rotationYaw = player.rotationYawHead
    eff.rotationPitch = player.rotationPitch

    world.spawnEntityInWorld(eff)

    playSound(point)
  }

  private def playSound(pos: net.minecraft.util.Vec3) = {
    ACSounds.playClient(world, pos.x, pos.y, pos.z, "vecmanip.vec_reflection", 0.5f, 1.0f)
  }

  @SubscribeEvent
  def onRenderOverlay(evt: RenderGameOverlayEvent) = {
    if (evt.`type` == ElementType.CROSSHAIRS) {
      val r = evt.resolution
      ui.onFrame(r.getScaledWidth, r.getScaledHeight)
    }
  }

}

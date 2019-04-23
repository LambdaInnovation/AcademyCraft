package cn.academy.ability.vanilla.vecmanip.skill

import java.util.function.Predicate

import cn.academy.ability.Skill
import cn.academy.ability.context.ClientRuntime.{ActivateHandlers, IActivateHandler}
import cn.academy.ability.context._
import cn.academy.ability.ctrl.KeyDelegates
import cn.academy.client.sound.ACSounds
import cn.academy.ability.vanilla.vecmanip.client.effect.{WaveEffect, WaveEffectUI}
import cn.academy.ability.vanilla.vecmanip.skill.EntityAffection.{Affected, Excluded}
import cn.academy.event.ability.ReflectEvent
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util.MathUtils._
import cn.lambdalib2.util.{Raytrace, SideUtils, WorldUtils}
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile._
import net.minecraft.util.{DamageSource, SoundCategory}
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.living.{LivingAttackEvent, LivingHurtEvent}

object VecReflection extends Skill("vec_reflection", 4) {

  MinecraftForge.EVENT_BUS.register(this)

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int): Unit = {
    rt.addKey(keyid, KeyDelegates.contextActivate(this, new VecReflectionContext(_)))
  }

}

import VecReflectionContext._
import cn.lambdalib2.util.RandUtils._
import cn.academy.ability.api.AbilityAPIExt._
import collection.mutable
import net.minecraft.util.math.Vec3d
import cn.lambdalib2.util.VecUtils._
import VMSkillHelper._

private object VecReflectionContext {
  final val MSG_EFFECT = "effect"
  final val MSG_REFLECT_ENTITY = "reflect_ent"

  def reflect(entity: Entity, player: EntityPlayer): Unit = {
    val lookPos = Raytrace.getLookingPos(player, 20).getLeft
    val speed = new Vec3d(entity.motionX, entity.motionY, entity.motionZ).length()
    val vel = multiply(subtract(lookPos, entityHeadPos(entity)).normalize, speed)
    entity.motionX = vel.x
    entity.motionY = vel.y
    entity.motionZ = vel.z
  }
}

class VecReflectionContext(p: EntityPlayer) extends Context(p, VecReflection) {

  private val visited = mutable.Set[Entity]()
  import scala.collection.JavaConversions._

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  def s_makeAlive(): Unit = {
    MinecraftForge.EVENT_BUS.register(this)
    ctx.consume(overloadToKeep, 0)
    overloadKeep = ctx.cpData.getOverload
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.SERVER, Side.CLIENT))
  def g_terminate(): Unit = {
    MinecraftForge.EVENT_BUS.unregister(this)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  def s_tick(): Unit = {
    if(ctx.cpData.getOverload < overloadKeep) ctx.cpData.setOverload(overloadKeep)
    val range = 4
    val entities = WorldUtils.getEntities(player, range, new Predicate[Entity] {
      override def test(t: Entity): Boolean = true
    })
    entities.removeAll(visited)

    entities.filterNot(EntityAffection.isMarked).foreach (entity => {
      EntityAffection.getAffectInfo(entity) match {
        case Affected(difficulty) =>
          entity match {
            case fireball : EntityFireball =>
              if(consumeEntity(difficulty)) {
                createNewFireball(fireball)

                ctx.addSkillExp(difficulty * 0.0008f)
                sendToClient(MSG_REFLECT_ENTITY, entity)
              }
            case _ =>
              if(consumeEntity(difficulty)) {
                reflect(entity, player)

                EntityAffection.mark(entity)

                ctx.addSkillExp(difficulty * 0.0008f)
                sendToClient(MSG_REFLECT_ENTITY, entity)
              }
          }
        case Excluded() =>
      }
    })

    visited ++= entities

    if(!consumeNormal)
      terminate()
  }

  private def createNewFireball(source : EntityFireball): Boolean = {
    source.setDead()

    val shootingEntity = source.shootingEntity
    var fireball : EntityFireball = null

    source match {
      case l : EntityLargeFireball =>
        fireball = new EntityLargeFireball(world(), shootingEntity, shootingEntity.posX,
          shootingEntity.posY, shootingEntity.posZ)
        fireball.asInstanceOf[EntityLargeFireball].explosionPower = l.explosionPower
      case _ =>
        source.shootingEntity match {
          case null =>
            fireball = new EntitySmallFireball(world(), source.posX, source.posY, source.posZ,
              source.posX, source.posY, source.posZ)
          case _ =>
            fireball = new EntitySmallFireball(world(), shootingEntity, shootingEntity.posX,
              shootingEntity.posY, shootingEntity.posZ)
        }
    }

    fireball.setPosition(source.posX, source.posY, source.posZ)
    val lookPos = Raytrace.getLookingPos(player, 20).getLeft
    val speed = new Vec3d(source.motionX, source.motionY, source.motionZ).length()
    val vel = multiply(subtract(lookPos, entityHeadPos(source)).normalize, speed)
    setMotion(fireball, vel)
    EntityAffection.mark(fireball)
    world().spawnEntity(fireball)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  def c_tick(): Unit = {
    if(!consumeNormal)
      terminate()
  }

  @SubscribeEvent
  def onReflect(evt: ReflectEvent): Unit = {
    if (evt.target.equals(player)) {
      evt.setCanceled(true)

      val dpos = subtract(entityHeadPos(evt.player), entityHeadPos(player))
      sendToClient(MSG_EFFECT, add(add(player.getPositionVector, new Vec3d(0, ranged(0.4, 1.3), 0)), multiply(dpos.normalize(), 0.5)))
    }
  }

  /**
    * Note: Canceling the damage event in `LivingHurtEvent` still causes knockback, so there needs
    *  to be one more pre testing.
    */
  @SubscribeEvent
  def onLivingAttack(evt: LivingAttackEvent): Unit = {
    if (evt.getEntityLiving.equals(player)) {
      val (performed, _) = handleAttack(evt.getSource, evt.getAmount, passby = true)
      if (performed) {
        handleAttack(evt.getSource, evt.getAmount, passby = false)
        evt.setCanceled(true)
      }
    }
  }

  @SubscribeEvent
  def onLivingHurt(evt: LivingHurtEvent): Unit = {
    if (evt.getEntityLiving.equals(player)  && evt.getAmount <=9999) {
      val (_, dmg) = handleAttack(evt.getSource, evt.getAmount, passby = false)
      evt.setAmount(dmg)
      if(dmg<=0){
        evt.setCanceled(true)
      }
    }
  }

  // Sometimes reflection will cause reentrant, e.g. when Guardian
  //   gives thorns damage to any of its attacks, or
  //   two players vector-reflect against each other.
  // Under these situation, we don't allow recursion of reflection.
  private var _isAttacking = false

  /**
    * @param passby If passby=true, and this isn't a complete absorb, the action will not perform. Else it will.
    * @return (Whether action had been really performed, processed damage)
    */
  private def handleAttack(dmgSource: DamageSource, dmg: Float, passby: Boolean): (Boolean, Float) = {
    val reflectDamage = lerpf(0.6f, 1.2f, ctx.getSkillExp) * dmg
    if (!passby) { // Perform the action.
      _isAttacking = true

      if (!_isAttacking) {
        consumeDamage(dmg)
        ctx.addSkillExp(dmg * 0.0004f)

        val sourceEntity = dmgSource.getImmediateSource
        if (sourceEntity != null && sourceEntity != player) {
          ctx.attack(sourceEntity, reflectDamage)

          if (!SideUtils.isClient)
            sendToClient(MSG_EFFECT, sourceEntity.getPositionVector)
        }
      }

      _isAttacking = false

      (true, dmg - reflectDamage)
    } else {
      (reflectDamage>=1, dmg - reflectDamage)
    }
  }

  private def consumeEntity(difficulty: Float) = {
    ctx.consume(0, difficulty * lerpf(300, 160, ctx.getSkillExp))
  }

  private def consumeDamage(damage: Float): Unit = ctx.consumeWithForce(0, lerpf(20, 15, ctx.getSkillExp) * damage)

  private def consumeNormal(): Boolean = {
    ctx.consume(0,  lerpf(15, 11, ctx.getSkillExp))
  }

  private val overloadToKeep = lerpf(350, 250, ctx.getSkillExp)
  private var overloadKeep = 0f

}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[VecReflectionContext])
class VecReflectionContextC(par: VecReflectionContext) extends ClientContext(par) {

  private var activateHandler: IActivateHandler = _
  private val ui = new WaveEffectUI(0.4f, 110, 1.6f)

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def l_alive(): Unit = if (isLocal) {
    activateHandler = ActivateHandlers.terminatesContext(par)
    ClientRuntime.instance.addActivateHandler(activateHandler)
    MinecraftForge.EVENT_BUS.register(this)
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def l_terminate(): Unit = if (isLocal) {
    ClientRuntime.instance.removeActiveHandler(activateHandler)
    MinecraftForge.EVENT_BUS.unregister(this)
  }

  @Listener(channel=MSG_REFLECT_ENTITY, side=Array(Side.CLIENT))
  private def c_reflectEntity(ent: Entity): Unit = {
    reflect(ent, player)
    reflectEffect(entityHeadPos(ent))
  }

  @Listener(channel=MSG_EFFECT, side=Array(Side.CLIENT))
  private def reflectEffect(point: Vec3d): Unit = {
    val eff = new WaveEffect(world, 2, 1.1)
    eff.setPosition(point.x, point.y, point.z)
    eff.rotationYaw = player.rotationYawHead
    eff.rotationPitch = player.rotationPitch

    world.spawnEntity(eff)

    playSound(point)
  }

  private def playSound(pos: net.minecraft.util.math.Vec3d): Unit = {
    ACSounds.playClient(world, pos.x, pos.y, pos.z, "vecmanip.vec_reflection", SoundCategory.AMBIENT, 0.5f, 1.0f)
  }

  @SubscribeEvent
  def onRenderOverlay(evt: RenderGameOverlayEvent): Unit = {
    if (evt.getType == ElementType.CROSSHAIRS) {
      val r = evt.getResolution
      ui.onFrame(r.getScaledWidth, r.getScaledHeight)
    }
  }

}
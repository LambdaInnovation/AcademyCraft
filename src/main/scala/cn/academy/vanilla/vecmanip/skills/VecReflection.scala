package cn.academy.vanilla.vecmanip.skills

import java.util.function.Predicate

import cn.academy.ability.api.{AbilityPipeline, Skill}
import cn.academy.ability.api.context._
import cn.academy.ability.api.event.ReflectEvent
import cn.academy.core.client.sound.ACSounds
import cn.academy.vanilla.vecmanip.client.effect.WaveEffect
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.MathUtils._
import cn.lambdalib.util.mc.{Raytrace, Vec3, WorldUtils}
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.command.IEntitySelector
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.{EntityArrow, EntityFireball}
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.living.LivingHurtEvent

object VecReflection extends Skill("vec_reflection", 4) {

  MinecraftForge.EVENT_BUS.register(this)

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new VecReflectionContext(p))

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

  val filter = new Predicate[Entity] {
    override def test(entity: Entity): Boolean = shouldReflect(entity)
  }

  def shouldReflect(e: Entity): Boolean = e match {
    case e: EntityArrow if !e.isInGround => true
    case _: EntityFireball => true
    case _ => false
  }

  def reflect(e: Entity, player: EntityPlayer): Unit = {
    val lookPos = Raytrace.getLookingPos(player, 20).getLeft
    val speed = e match {
      case _ : EntityArrow => 2
      case _ : EntityFireball => 0.5
      case _ => 1
    }

    val vel = (lookPos - e.headPosition).normalize() * speed

    e.setVel(vel)
  }
}

class VecReflectionContext(p: EntityPlayer) extends Context(p, VecReflection) {

  private val visited = mutable.Set[Entity]()

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  def s_makeAlive() = {
    MinecraftForge.EVENT_BUS.register(this)
  }

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  def l_keyUp() = {
    terminate()
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  def l_keyAbort() = {
    terminate()
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.SERVER, Side.CLIENT))
  def g_terminate() = {
    MinecraftForge.EVENT_BUS.unregister(this)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  def s_tick() = {
    val range = 4
    val entities = WorldUtils.getEntities(player, range, filter)
    entities.removeAll(visited)
    entities foreach (entity => {
      if (consumeEntity()) {
        reflect(entity, player)
        sendToClient(MSG_REFLECT_ENTITY, entity)
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

  @SubscribeEvent
  def onLivingHurt(evt: LivingHurtEvent) = {
    if (evt.entityLiving.equals(player)) {

      if (consumeDamage()) {
        val rate = reflectRate
        val returned = evt.ammount * rate
        val applied = math.max(evt.ammount - returned, 0)

        evt.ammount = applied

        if (evt.source.getEntity != null) { // Return the damage to the applier
          ctx.attack(evt.source.getEntity, returned)
          sendToClient(MSG_EFFECT, evt.source.getEntity.position)
        }

        if (evt.ammount == 0) {
          evt.setCanceled(true)
        }
      }

    }
  }

  private def reflectRate = lerpf(0.7f, 1.2f, ctx.getSkillExp) * rangef(0.9f, 1.1f)

  private def consumeEntity() = ctx.consume(lerpf(30, 16, ctx.getSkillExp), lerpf(300, 160, ctx.getSkillExp))

  private def consumeDamage() = ctx.consume(lerpf(30, 10, ctx.getSkillExp), lerpf(300, 200, ctx.getSkillExp))

  private def consumeNormal() = ctx.consume(lerpf(2, 1.5f, ctx.getSkillExp), lerpf(20, 16, ctx.getSkillExp))

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[VecReflectionContext])
class VecReflectionContextC(par: VecReflectionContext) extends ClientContext(par) {

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

}

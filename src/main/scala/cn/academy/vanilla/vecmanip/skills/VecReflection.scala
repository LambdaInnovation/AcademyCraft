package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.{AbilityPipeline, Skill}
import cn.academy.ability.api.context.{ClientRuntime, Context, ContextManager}
import cn.academy.ability.api.event.ReflectEvent
import cn.academy.vanilla.vecmanip.client.effect.WaveEffect
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

object VecReflectionContext {
  final val MSG_EFFECT = "effect"
  final val MSG_REFLECT_ENTITY = "reflect_ent"

  val acceptedTypes = List(classOf[EntityArrow], classOf[EntityFireball])

  val filter = new IEntitySelector {
    override def isEntityApplicable(entity: Entity): Boolean = shouldReflect(entity)
  }

  def shouldReflect(e: Entity) = acceptedTypes.exists(_.isInstance(e))

  def reflect(e: Entity, player: EntityPlayer) = {
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

class VecReflectionContext(p: EntityPlayer) extends Context(p) {

  val visited = mutable.Set[Entity]()

  implicit val aData_ = aData()
  implicit val skill_ = VecReflection

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

  @SideOnly(Side.CLIENT)
  @Listener(channel=MSG_EFFECT, side=Array(Side.CLIENT))
  def reflectEffect(point: MCVec3) = {
    val eff = new WaveEffect(world, 2, 1.1)
    eff.setPos(point)
    eff.rotationYaw = player.rotationYawHead
    eff.rotationPitch = player.rotationPitch

    world.spawnEntityInWorld(eff)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  def s_tick() = {
    val range = 6
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

  @Listener(channel=MSG_REFLECT_ENTITY, side=Array(Side.CLIENT))
  def c_reflectEntity(ent: Entity) = {
    reflect(ent, player)
    reflectEffect(ent.headPosition)
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
          AbilityPipeline.attack(player, skill_, evt.source.getEntity, returned)
          sendToClient(MSG_EFFECT, evt.source.getEntity.position)
        }

        if (evt.ammount == 0) {
          evt.setCanceled(true)
        }
      }

    }
  }

  private def reflectRate = lerpf(0.7f, 1.2f, skillExp) * rangef(0.9f, 1.1f)

  private def consumeEntity() = cpData.perform(lerpf(30, 16, skillExp), lerpf(300, 160, skillExp))

  private def consumeDamage() = cpData.perform(lerpf(30, 10, skillExp), lerpf(300, 200, skillExp))

  private def consumeNormal() = cpData.perform(lerpf(2, 1.5f, skillExp), lerpf(20, 16, skillExp))

}

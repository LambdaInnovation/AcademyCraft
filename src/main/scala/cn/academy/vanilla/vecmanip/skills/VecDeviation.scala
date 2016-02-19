package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{ContextManager, Context, ClientRuntime}
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.mc.WorldUtils
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.Side
import net.minecraft.command.IEntitySelector
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.EntityArrow
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.living.{LivingHurtEvent, LivingAttackEvent}

object VecDeviation extends Skill("vec_deviation", 2) {

  MinecraftForge.EVENT_BUS.register(this)

  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new VecDeviationContext(p))

  @SubscribeEvent
  def onLivingHurt(evt: LivingHurtEvent) = evt.entityLiving match {
    case player: EntityPlayer =>
      val ctx = ContextManager.instance.find(classOf[VecDeviationContext])
      if (ctx.isPresent) {
        val reduce = ctx.get.reduceDamage(evt.ammount)
        evt.ammount = reduce
      }
    case _ =>
  }

}

object VecDeviationContext {

  final val MSG_STOP_ENTITY = "stop_ent"

  val acceptedTypes = List(classOf[EntityArrow])

  def shouldStop(e: Entity) = acceptedTypes.exists(_.isInstance(e))

  def stop(e: Entity) = e match {
    case ent: EntityArrow =>
      ent.motionX = 0
      ent.motionY = 0
      ent.motionZ = 0
      ent.setDamage(0)
  }

  val stopFilter = new IEntitySelector {
    override def isEntityApplicable(entity: Entity): Boolean = shouldStop(entity)
  }

}

import cn.lambdalib.util.mc.MCExtender._
import cn.academy.ability.api.AbilityAPIExt._
import collection.mutable
import VecDeviationContext._
import scala.collection.JavaConversions._

class VecDeviationContext(p: EntityPlayer) extends Context(p) {

  val visited = mutable.Set[Entity]()

  @Listener(channel=MSG_KEYDOWN, side=Array(Side.CLIENT))
  def l_keyDown() = {}

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  def l_keyUp() = {
    terminate()
  }

  def reduceDamage(dmg: Float) = {
    println("ReduceDamage")
    dmg * 0.65f
  }

  def shouldStopEntity(ent: Entity) = true && shouldStop(ent)

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  def s_tick() = {
    // Check the entities around player, and stop them by probablity
    val range = 5
    val entities = WorldUtils.getEntities(player, range, stopFilter)
    entities.removeAll(visited)
    entities foreach (entity => {
      if (shouldStopEntity(entity)) {
        stop(entity)
        sendToClient(MSG_STOP_ENTITY, entity)
      }
    })

    visited ++= entities
  }

  @Listener(channel=MSG_STOP_ENTITY, side=Array(Side.CLIENT))
  def c_stopEntity(ent: Entity) = {
    stop(ent)
  }

}
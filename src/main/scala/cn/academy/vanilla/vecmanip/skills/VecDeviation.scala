package cn.academy.vanilla.vecmanip.skills

import java.util.function.Predicate

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{ClientRuntime, Context, ContextManager}
import cn.academy.vanilla.vecmanip.client.effect.WaveEffect
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.mc.WorldUtils
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.command.IEntitySelector
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.{EntityArrow, EntityFireball}
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.living.{LivingAttackEvent, LivingHurtEvent}
import VMSkillHelper._
import cn.academy.core.client.sound.ACSounds

object VecDeviation extends Skill("vec_deviation", 2) {

  MinecraftForge.EVENT_BUS.register(this)

  @SideOnly(Side.CLIENT)
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
  final val MSG_PLAY = "playsnd"

  def shouldStop(e: Entity): Boolean = e match {
    case e: EntityArrow if !e.isInGround => true
    case e: EntityFireball => true
    case _ => false
  }

  def stop(e: Entity, player: EntityPlayer) = {
    e match {
      case _ if isMarked(e) => // Do nothing if already deviated
      case ent: EntityArrow =>
        ent.motionX = 0
        ent.motionY = 0
        ent.motionZ = 0
        ent.setDamage(0)
      case ent: EntityFireball =>
        val dx = player.posX - e.posX
        val dz = player.posZ - e.posZ
        val nl = math.sqrt(dx * dx + dz * dz)

        ent.motionX = -dx / nl * 0.3
        ent.motionZ = -dz / nl * 0.3

        ent.motionY = -0.5
    }
    mark(e)
  }

  val stopFilter = new Predicate[Entity] {
    override def test(entity: Entity): Boolean = shouldStop(entity)
  }

  def mark(targ: Entity) = targ.getEntityData.setBoolean("ac_vm_deviated", true)
  def isMarked(targ: Entity) = targ.getEntityData.getBoolean("ac_vm_deviated")

}

import cn.lambdalib.util.mc.MCExtender._
import cn.academy.ability.api.AbilityAPIExt._
import collection.mutable
import scala.collection.JavaConversions._

class VecDeviationContext(p: EntityPlayer) extends Context(p) {
  import cn.lambdalib.util.generic.MathUtils._
  import VecDeviationContext._

  private implicit val aData_ = aData()
  private implicit val skill_ = VecDeviation

  val visited = mutable.Set[Entity]()

  @Listener(channel=MSG_KEYDOWN, side=Array(Side.CLIENT))
  def l_keyDown() = {}

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  def l_keyUp() = terminate()

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  def l_keyAbort() = terminate()

  def reduceDamage(dmg: Float) = {
    val consumpRatio = 60.0f
    val overloadRatio = 10.0f

    val consumption = math.min(cpData.getCP, dmg * consumpRatio)
    val acceptedDamage = consumption / consumpRatio
    val absorbed = acceptedDamage * 0.75f

    cpData.perform(consumption, acceptedDamage * overloadRatio)
    addSkillExp(0.004f)

    sendToClient(MSG_PLAY, player.position)

    dmg - absorbed
  }

  def shouldStopEntity(ent: Entity) = shouldStop(ent)

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  def s_tick() = {
    // Check the entities around player, and stop them by probablity
    val range = 5
    val entities = WorldUtils.getEntities(player, range, stopFilter)
    entities.removeAll(visited)
    entities foreach (entity => {
      if (shouldStopEntity(entity) && consumeStop()) {
        stop(entity, player)
        addSkillExp(0.001f)

        sendToClient(MSG_STOP_ENTITY, entity)
      }
    })

    visited ++= entities
  }

  @Listener(channel=MSG_TICK, side={Array(Side.CLIENT, Side.SERVER)})
  def g_tick() = if (!isRemote || isLocal) {
    val normConsume = lerpf(5, 2.5f, skillExp)
    val normOverload = lerpf(0.5f, 0.2f, skillExp)
    cpData.perform(normOverload, normConsume)
  }

  @SideOnly(Side.CLIENT)
  @Listener(channel=MSG_STOP_ENTITY, side=Array(Side.CLIENT))
  def c_stopEntity(ent: Entity) = {
    if (!isMarked(ent)) {
      val eff = new WaveEffect(world, 1, 0.6)
      eff.setPos(ent.headPosition)
      eff.rotationYaw = player.rotationYaw
      eff.rotationPitch = player.rotationPitch

      world.spawnEntityInWorld(eff)
      playSound(eff.position)
    }

    stop(ent, player)
  }

  def consumeStop() = {
    cpData.perform(
      lerpf(16, 10, skillExp),
      lerpf(150, 100, skillExp))
  }

  @SideOnly(Side.CLIENT)
  @Listener(channel=MSG_PLAY, side=Array(Side.CLIENT))
  private def playSound(pos: net.minecraft.util.Vec3) = {
    ACSounds.playClient(world, pos.x, pos.y, pos.z, "vecmanip.vec_deviation", 0.5f, 1.0f)
  }

}
package cn.academy.vanilla.vecmanip.skills

import java.util.function.Predicate

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context._
import cn.academy.vanilla.vecmanip.client.effect.{WaveEffect, WaveEffectUI}
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.mc.WorldUtils
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.{EntityLargeFireball, EntityArrow, EntityFireball}
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.living.{LivingAttackEvent, LivingHurtEvent}
import cn.academy.ability.api.context.ClientRuntime.{ActivateHandlers, IActivateHandler}
import cn.academy.ability.api.ctrl.KeyDelegates
import cn.academy.core.client.sound.ACSounds
import cn.academy.vanilla.vecmanip.skills.EntityAffection.{Affected, Excluded}
import cn.lambdalib.annoreg.core.Registrant
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType

object VecDeviation extends Skill("vec_deviation", 2) {

  MinecraftForge.EVENT_BUS.register(this)

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = {
    rt.addKey(keyid, KeyDelegates.contextActivate(this, new VecDeviationContext(_)))
  }

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

}

import cn.lambdalib.util.mc.MCExtender._
import cn.academy.ability.api.AbilityAPIExt._
import collection.mutable
import scala.collection.JavaConversions._
import cn.lambdalib.util.generic.MathUtils._
import VecDeviationContext._

class VecDeviationContext(p: EntityPlayer) extends Context(p, VecDeviation) {

  private val visited = mutable.Set[Entity]()
  private val markedWhitelist = { classOf[EntityLargeFireball] }
  private var ticker = 0

  private[skills] def reduceDamage(dmg: Float) = {
    val consumpRatio = 60.0f
    val overloadRatio = 10.0f

    val consumption = math.min(ctx.cpData.getCP, dmg * consumpRatio)
    val acceptedDamage = consumption / consumpRatio
    val absorbed = acceptedDamage * 0.75f

    ctx.consume(consumption, acceptedDamage * overloadRatio)
    ctx.addSkillExp(dmg * 0.0006f)

    sendToClient(MSG_PLAY, player.position)

    dmg - absorbed
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_tick() = {
    // Check the entities around player, and stop them by probablity
    val range = 5

    val entities = WorldUtils.getEntities(player, range,
      new Predicate[Entity] { override def test(t: Entity) = true })
    entities.removeAll(visited)

    entities.filterNot(EntityAffection.isMarked).foreach(entity =>  {
      val info = EntityAffection.getAffectInfo(entity)
      info match {
        case Affected(difficulty) => // Process not-marked and affected entities
          if (consumeStop(difficulty)) {
            entity match {
              case arrow: EntityArrow =>
                arrow.setDamage(0)
              case _ =>
            }

            entity.motionX = 0
            entity.motionY = 0
            entity.motionZ = 0

            ctx.addSkillExp(0.001f * difficulty)

            sendToClient(MSG_STOP_ENTITY, entity)

            EntityAffection.mark(entity)
          }
        case Excluded(difficulty) =>
          if(ticker == 5 && markedWhitelist.getClasses.contains(entity.getClass)) {
            if (consumeStop(difficulty)) {
              entity match {
                case arrow: EntityArrow =>
                  arrow.setDamage(0)
                case _ =>
              }

              entity.motionX = 0
              entity.motionY = 0
              entity.motionZ = 0

              ctx.addSkillExp(0.001f * difficulty)

              sendToClient(MSG_STOP_ENTITY, entity)

              EntityAffection.mark(entity)
            }
          }
      }
    })

    visited ++= entities
    ticker += 1
  }

  @Listener(channel=MSG_TICK, side={Array(Side.CLIENT, Side.SERVER)})
  private def g_tick() = if (!isRemote || isLocal) {
    val normConsume = lerpf(5, 2.5f, ctx.getSkillExp)
    val normOverload = lerpf(0.5f, 0.2f, ctx.getSkillExp)
    ctx.consume(normOverload, normConsume)
  }

  private def consumeStop(difficulty: Float) = {
    ctx.consume(
      difficulty * lerpf(16, 10, ctx.getSkillExp),
      difficulty * lerpf(150, 100, ctx.getSkillExp))
  }

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[VecDeviationContext])
class VecDeviationContextC(par: VecDeviationContext) extends ClientContext(par) {

  private var activateHandler: IActivateHandler = _
  private val ui = new WaveEffectUI(0.2f, 100, 1.4f)

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

  @Listener(channel=MSG_STOP_ENTITY, side=Array(Side.CLIENT))
  def c_stopEntity(ent: Entity) = {
    if (EntityAffection.isMarked(ent)) {
      val eff = new WaveEffect(world, 1, 0.6)
      eff.setPos(ent.headPosition)
      eff.rotationYaw = player.rotationYaw
      eff.rotationPitch = player.rotationPitch

      world.spawnEntityInWorld(eff)
      playSound(eff.position)
    }

    ent.motionX = 0
    ent.motionY = 0
    ent.motionZ = 0
  }

  @Listener(channel=MSG_PLAY, side=Array(Side.CLIENT))
  private def playSound(pos: net.minecraft.util.Vec3) = {
    ACSounds.playClient(world, pos.x, pos.y, pos.z, "vecmanip.vec_deviation", 0.5f, 1.0f)
  }

  @SubscribeEvent
  def onRenderOverlay(evt: RenderGameOverlayEvent) = {
    if (evt.`type` == ElementType.CROSSHAIRS) {
      val r = evt.resolution
      ui.onFrame(r.getScaledWidth, r.getScaledHeight)
    }
  }

}
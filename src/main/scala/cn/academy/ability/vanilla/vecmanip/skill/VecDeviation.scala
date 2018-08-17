package cn.academy.ability.vanilla.vecmanip.skill

import java.util.function.Predicate

import cn.academy.ability.Skill
import cn.academy.ability.context._
import cn.academy.ability.vanilla.vecmanip.client.effect.{WaveEffect, WaveEffectUI}
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.{EntityArrow, EntityLargeFireball, EntitySmallFireball}
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.living.LivingHurtEvent
import cn.academy.ability.context.ClientRuntime.{ActivateHandlers, IActivateHandler}
import cn.academy.ability.ctrl.KeyDelegates
import cn.academy.client.sound.ACSounds
import cn.academy.ability.vanilla.vecmanip.skill.EntityAffection.{Affected, Excluded}
import cn.lambdalib2.util.{VecUtils, WorldUtils}
import net.minecraft.util.SoundCategory
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType

object VecDeviation extends Skill("vec_deviation", 2) {

  MinecraftForge.EVENT_BUS.register(this)

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = {
    rt.addKey(keyid, KeyDelegates.contextActivate(this, new VecDeviationContext(_)))
  }

  @SubscribeEvent
  def onLivingHurt(evt: LivingHurtEvent) = evt.getEntity match {
    case player: EntityPlayer =>
      val ctx = ContextManager.instance.find(classOf[VecDeviationContext])
      if (ctx.isPresent && evt.getAmount<=9999) {
        val reduce = ctx.get.reduceDamage(evt.getAmount)
        evt.setAmount(reduce)
      }
    case _ =>
  }

}

object VecDeviationContext {

  final val MSG_STOP_ENTITY = "stop_ent"
  final val MSG_PLAY = "playsnd"

}

import cn.academy.ability.api.AbilityAPIExt._
import collection.mutable
import scala.collection.JavaConversions._
import cn.lambdalib2.util.MathUtils._
import VecDeviationContext._

class VecDeviationContext(p: EntityPlayer) extends Context(p, VecDeviation) {

  private val visited = mutable.Set[Entity]()

  private[skill] def reduceDamage(dmg: Float) = {
    val consumption = math.min(ctx.cpData.getCP, lerpf(15, 12, ctx.getSkillExp))

    ctx.consume(0, consumption)
    ctx.addSkillExp(dmg * 0.0006f)

    sendToClient(MSG_PLAY, player.getPositionVector)

    dmg * (1 - lerpf(0.4f, 0.9f, ctx.getSkillExp))
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  private def s_madeAlive() = {
    ctx.consume(overloadToKeep, 0)
    overloadKeep = ctx.cpData.getOverload
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_tick() = {
    if(!ctx.consume(0, tickConsumption)) terminate()
    if(ctx.cpData.getOverload < overloadKeep) ctx.cpData.setOverload(overloadKeep)
    // Check the entities around player, and stop them by probablity
    val range = 5

    val entities = WorldUtils.getEntities(player, range,
      new Predicate[Entity] { override def test(t: Entity) = true })
    entities.removeAll(visited)

    entities.filterNot(EntityAffection.isMarked).foreach(entity =>  {
      EntityAffection.getAffectInfo(entity) match {
        case Affected(difficulty) => // Process not-marked and affected entities
          ctx.consumeWithForce(0, comsumption)
          entity match {
            case lfireball : EntityLargeFireball =>
              lfireball.setDead()
              world.newExplosion(null, lfireball.posX, lfireball.posY, lfireball.posZ,
                lfireball.explosionPower, true, world.getGameRules.getBoolean("mobGriefing"))

              ctx.addSkillExp(0.001f * difficulty)
              sendToClient(MSG_STOP_ENTITY, lfireball)
            case sfireball : EntitySmallFireball =>
              sfireball.setDead()

              ctx.addSkillExp(0.001f * difficulty)
              sendToClient(MSG_STOP_ENTITY, sfireball)
            case _ =>
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
        case Excluded() =>
      }
    })

    visited ++= entities
  }

  @Listener(channel=MSG_TICK, side={Array(Side.CLIENT, Side.SERVER)})
  private def g_tick() = if (!isRemote || isLocal) {
    val normConsume = lerpf(5, 2.5f, ctx.getSkillExp)
    val normOverload = lerpf(0.5f, 0.2f, ctx.getSkillExp)
    ctx.consume(normOverload, normConsume)
  }

  private val tickConsumption = lerpf(13, 5, ctx.getSkillExp)
  private val comsumption = lerpf(15, 12, ctx.getSkillExp)
  private val overloadToKeep = lerpf(80, 50, ctx.getSkillExp)
  private var overloadKeep = 0f

}

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
      val pos = VecUtils.entityHeadPos(ent)
      eff.setPosition(pos.x, pos.y, pos.z)
      eff.rotationYaw = player.rotationYaw
      eff.rotationPitch = player.rotationPitch

      world.spawnEntity(eff)
      playSound(eff.getPositionVector)
    }

    ent.motionX = 0
    ent.motionY = 0
    ent.motionZ = 0
  }

  @Listener(channel=MSG_PLAY, side=Array(Side.CLIENT))
  private def playSound(pos: net.minecraft.util.math.Vec3d) = {
    ACSounds.playClient(world, pos.x, pos.y, pos.z, "vecmanip.vec_deviation", SoundCategory.AMBIENT, 0.5f, 1.0f)
  }

  @SubscribeEvent
  def onRenderOverlay(evt: RenderGameOverlayEvent) = {
    if (evt.getType == ElementType.CROSSHAIRS) {
      val r = evt.getResolution
      ui.onFrame(r.getScaledWidth, r.getScaledHeight)
    }
  }

}
package cn.academy.ability.vanilla.meltdowner.skill

import cn.academy.ability.Skill
import cn.academy.ability.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.client.render.particle.MdParticleFactory
import cn.academy.entity.{EntityDiamondShield, EntityRippleMark}
import cn.lambdalib2.particle.Particle
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util.{RandUtils, VecUtils}
import cn.lambdalib2.util.{EntitySelectors, Raytrace}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.{RayTraceResult, Vec3d}

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
import cn.lambdalib2.util.MathUtils._
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
    sendToSelf(MSG_MARK_END)
    terminate()
  }

  @Listener(channel=MSG_MARK_END, side=Array(Side.SERVER))
  private def s_onEnd() = {
    if(ctx.consume(consumption, overload)) {
      sendToClient(MSG_MARK_END)
      sendToSelf(MSG_TRIGGER, getDest)
      ctx.addSkillExp(.004f)
      ctx.setCooldown(lerpf(60, 30, exp).toInt)
    } else {
      sendToClient(MSG_MARK_END)
      terminate()
    }
  }

  private def getDest: Vec3d = Raytrace.getLookingPos(player, 12, EntitySelectors.nothing).getLeft

  //TRIGGER
  private val TIME: Float = 8
  private val LIFETIME: Float = 15
  private var target: Vec3d = _
  private var ticks: Int = 0
  private var isTriggering = false

  private var start: Vec3d = _
  private var velocity: Vec3d = _

  @Listener(channel=MSG_TRIGGER, side=Array(Side.SERVER))
  private def s_triggerStart(_target: Vec3d) = {
    target = _target
    isTriggering = true

    sendToClient(MSG_TRIGGER, target)
  }

  @Listener(channel=MSG_TRIGGER, side=Array(Side.CLIENT))
  private def c_triggerStart(_target: Vec3d) = {
    if(isLocal) {
      isTriggering = true
      target = _target

      start = player.getPositionVector
      velocity = VecUtils.multiply(VecUtils.subtract(target, start), 1.0 / TIME)
    }
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_triggerTick() = {
    if(isTriggering) {
      val pos: RayTraceResult = Raytrace.perform(world,
        new Vec3d(player.lastTickPosX, player.lastTickPosY, player.lastTickPosZ),
        new Vec3d(player.posX, player.posY, player.posZ), EntitySelectors.exclude(player).and(EntitySelectors.living))
      if(player.getRidingEntity!=null)player.dismountRidingEntity()
      if (pos != null && pos.entityHit != null) MDDamageHelper.attack(ctx, pos.entityHit, lerpf(7, 20, exp))
    }
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def c_triggerTick(): Unit = {
    if(isLocal && isTriggering) {
      if (ticks >= LIFETIME)
        terminate()
      ticks += 1
      val pos: Vec3d = VecUtils.lerp(start, target, ticks / TIME)
      player.setPosition(pos.x, pos.y, pos.z)
      player.motionX = velocity.x
      player.motionY = velocity.y
      player.motionZ = velocity.z
      player.fallDistance = 0.0f
    }
  }

}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[JEContext])
class JEContextC(par: JEContext) extends ClientContext(par) {

  private val TIME: Float = 8
  
  private var mark: EntityRippleMark = _

  private var target: Vec3d = _
  private val start: Vec3d = new Vec3d(player.posX, player.posY, player.posZ)
  
  private var isMarking = false
  private var ticks: Int = 0
  
  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def l_spawnMark() = {
    if(isLocal) {
      isMarking = true
      mark = new EntityRippleMark(world)
      world.spawnEntity(mark)
      mark.color.set(51, 255, 51, 179)
    }
  }
  
  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def l_updateMark() = {
    if(isLocal && isMarking) {
      val dest: Vec3d = getDest
      mark.setPosition(dest.x, dest.y, dest.z)
    }
  }

  @Listener(channel=MSG_MARK_END, side=Array(Side.CLIENT))
  private def l_endMark() = {
    if(isLocal) {
      isMarking = false
      mark.setDead()
    }
  }
  
  private def getDest: Vec3d = Raytrace.getLookingPos(player, 12, EntitySelectors.nothing).getLeft

  //TRIGGER
  private var entity: EntityDiamondShield = _
  private var isTriggering = false

  @Listener(channel=MSG_TRIGGER, side=Array(Side.CLIENT))
  private def c_tStartEffect(_target: Vec3d) = {
    target = _target
    isTriggering = true
    entity = new EntityDiamondShield(player)
    world.spawnEntity(entity)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def c_tUpdateEffect() = {
    if(isTriggering) {
      ticks += 1
      if (isLocal) player.capabilities.setPlayerWalkSpeed(0.07f)
      for (i <- 0 to 10) {
        val pos2: Vec3d = VecUtils.lerp(start, target, 3 * ticks / TIME)
        val p: Particle = MdParticleFactory.INSTANCE.next(world, VecUtils.add(new Vec3d(player.posX, player.posY, player.posZ),
          new Vec3d(RandUtils.ranged(-.3, .3), RandUtils.ranged(-.3, .3), RandUtils.ranged(-.3, .3))),
          new Vec3d(RandUtils.ranged(-.02, .02), RandUtils.ranged(-.02, .02), RandUtils.ranged(-.02, .02)))
        world.spawnEntity(p)
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
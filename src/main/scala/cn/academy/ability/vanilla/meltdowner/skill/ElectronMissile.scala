package cn.academy.ability.vanilla.meltdowner.skill

import cn.academy.ability.Skill
import cn.academy.ability.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.client.render.particle.MdParticleFactory
import cn.academy.client.render.util.ACRenderingHelper
import cn.academy.entity.{EntityMdBall, EntityMdRaySmall}
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util.{MathUtils, VecUtils}
import cn.lambdalib2.util.MathUtils._
import cn.lambdalib2.util.{EntitySelectors, WorldUtils}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.Vec3d

/**
  * @author WeAthFolD, KSkun
  */
object ElectronMissile extends Skill("electron_missile", 5) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyID: Int) = activateSingleKey(rt, keyID, p => new EMContext(p))
  
}

object EMContext {

  final val MSG_EFFECT_SPAWN = "effect_spawn"
  final val MSG_EFFECT_UPDATE = "effect_update"

}

import cn.academy.ability.api.AbilityAPIExt._
import cn.lambdalib2.util.RandUtils._
import scala.collection.JavaConversions._
import EMContext._

class EMContext(p: EntityPlayer) extends Context(p, ElectronMissile) {

  private val MAX_HOLD: Int = 5
  
  private var active: java.util.LinkedList[EntityMdBall] = _
  private var ticks: Int = 0

  private val exp: Float = ctx.getSkillExp

  private val consumption: Float = lerpf(12, 5, exp)
  private val overload_attacked: Float = lerpf(9, 4, exp)
  private val consumption_attacked: Float = lerpf(60, 25, exp)
  private val overload_keep = 200

  private var overloadKeep = 0f

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
    ctx.consume(overload_keep, 0)
    overloadKeep = ctx.cpData.getOverload
    active = new java.util.LinkedList[EntityMdBall]()
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_onTick() = {
    if(ctx.cpData.getOverload < overloadKeep) ctx.cpData.setOverload(overloadKeep)
    if (!ctx.consume(0, consumption)) terminate()
    else {
      val timeLimit: Int = lerpf(80, 200, exp).toInt
      if (ticks <= timeLimit) {
        if (ticks % 10 == 0) if (active.size < MAX_HOLD) {
          val ball: EntityMdBall = new EntityMdBall(player)
          player.world.spawnEntity(ball)
          active.add(ball)
        }
        if (ticks != 0 && ticks % 8 == 0) {
          val range: Float = lerpf(5, 13, exp)
          val list: java.util.List[Entity] = WorldUtils.getEntities(
            player, range,
            EntitySelectors.exclude(player).and(EntitySelectors.living)
          )
          if (!active.isEmpty && !list.isEmpty && ctx.consume(overload_attacked, consumption_attacked)) {
            var min: Double = Double.MaxValue
            var result: Entity = null
            import scala.collection.JavaConversions._
            for (e <- list) {
              val dist: Double = e.getDistanceSq(player)
              if (dist < min) {
                min = dist
                result = e
              }
            }
            // Find a random ball and destroy it
            var index: Int = 1 + nextInt(active.size)
            val iter: java.util.Iterator[EntityMdBall] = active.iterator
            var ball: EntityMdBall = null
            while ( {
              index -= 1; index + 1
            } > 0) ball = iter.next
            iter.remove()
            // client action
            sendToClient(MSG_EFFECT_SPAWN, VecUtils.entityPos(ball), VecUtils.add(VecUtils.entityPos(result),
              new Vec3d(0, result.getEyeHeight, 0)))
            // server action
            result.hurtResistantTime = -1
            val damage: Float = lerpf(10, 18, exp)
            MDDamageHelper.attack(ctx, result, damage)
            ctx.addSkillExp(0.001f)
            ball.setDead()
          }
        }
      } else {
        // ticks > timeLimit
        terminate()
      }
      sendToClient(MSG_EFFECT_UPDATE)
      ticks += 1
    }
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.SERVER))
  private def s_onEnd() = {
    val cooldown: Int = MathUtils.clampi(700, 400, exp.toInt)
    ctx.setCooldown(cooldown)

    for (ball <- active) {
      ball.setDead()
    }
  }
  
}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[EMContext])
class EMContextC(par: EMContext) extends ClientContext(par) {

  @Listener(channel=MSG_EFFECT_UPDATE, side=Array(Side.CLIENT))
  private def c_updateEffect() = {
    var count: Int = rangei(1, 3)
    while ( {
      count -= 1; count + 1
    } > 0) {
      val r: Double = ranged(0.5, 1)
      val theta: Double = ranged(0, Math.PI * 2)
      val h: Double = ranged(-1.2, 0)
      val pos: Vec3d = VecUtils.add(new Vec3d(player.posX, player.posY + ACRenderingHelper.getHeightFix(player),
        player.posZ), new Vec3d(r * Math.sin(theta), h, r * Math.cos(theta)))
      val vel: Vec3d = new Vec3d(ranged(-.02, .02), ranged(.01, .05), ranged(-.02, .02))
      player.world.spawnEntity(MdParticleFactory.INSTANCE.next(player.world, pos, vel))
    }
  }

  @Listener(channel=MSG_EFFECT_SPAWN, side=Array(Side.CLIENT))
  private def c_spawnRay(from: Vec3d, to: Vec3d) = {
    val ray: EntityMdRaySmall = new EntityMdRaySmall(world)
    ray.setFromTo(from, to)
    world.spawnEntity(ray)
  }

}
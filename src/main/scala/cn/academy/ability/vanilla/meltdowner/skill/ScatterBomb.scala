package cn.academy.ability.vanilla.meltdowner.skill

import java.util
import java.util.function.Predicate

import cn.academy.ability.Skill
import cn.academy.ability.context.{ClientRuntime, Context}
import cn.academy.client.render.util.ACRenderingHelper
import cn.academy.entity.EntityMdBall
import cn.academy.network.NetworkManager
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util.{RandUtils, VecUtils}
import cn.lambdalib2.util.Motion3D
import cn.lambdalib2.util.mc.{EntitySelectors, Raytrace, WorldUtils}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.{Entity, EntityLiving, EntityLivingBase}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.{DamageSource, MovingObjectPosition, Vec3d}

/**
  * @author WeAthFolD, KSkun, Paindar
  */
object ScatterBomb extends Skill("scatter_bomb", 2) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyID: Int) = activateSingleKey(rt, keyID, p => new SBContext(p))

}

object SBContext {

  final val MSG_SYNC_BALLS = "sync_balls"

}

import cn.lambdalib2.util.MathUtils._
import cn.academy.ability.api.AbilityAPIExt._
import SBContext._

class SBContext(p: EntityPlayer) extends Context(p, ScatterBomb) {

  private def getDamage(exp: Float) = lerpf(5, 9, exp)

  private val balls: java.util.List[EntityMdBall] = new java.util.ArrayList[EntityMdBall]
  private val basicSelector: Predicate[Entity] = EntitySelectors.everything
  private val MAX_TICKS: Int = 80
  private val MOD: Int = 10
  private val RAY_RANGE: Double = 15
  private var ticks: Int = 0
  private val exp: Float = ctx.getSkillExp

  private var overloadKeep = 0f

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def l_onKeyUp() = {
    terminate()
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  private def l_onAbort() = {
    terminate()
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  private def s_onStart() = {
    val overload: Float = lerpf(80, 60, exp)
    ctx.consume(overload, 0)
    overloadKeep = ctx.cpData.getOverload
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_onTick() = {
    if(ctx.cpData.getOverload < overloadKeep) ctx.cpData.setOverload(overloadKeep)
    ticks += 1

    if (ticks <= 80) {
      if (ticks >= 20 && ticks % MOD == 0) {
        val ball: EntityMdBall = new EntityMdBall(player)
        world.spawnEntityInWorld(ball)
        balls.add(ball)
        sendToClient(MSG_SYNC_BALLS, ball)
      }
      val cp: Float = lerpf(3, 6, exp)
      if (!ctx.consume(0, cp)) terminate()
    }
    if (ticks == 200) {
      player.attackEntityFrom(DamageSource.causePlayerDamage(player), 6)
      terminate()
    }
  }

  @SideOnly(Side.CLIENT)
  @Listener(channel=MSG_SYNC_BALLS, side=Array(Side.CLIENT))
  private def c_syncBalls(ballToAdd: EntityMdBall) = balls.add(ballToAdd)

  @Listener(channel=MSG_TERMINATED, side=Array(Side.SERVER))
  private def s_onEnd() = {
    import scala.collection.JavaConversions._
    var autoCount = if (exp > 0.5) (balls.size().toFloat * exp).toInt else 0
    val autoTarget = if (exp > 0.5)
      WorldUtils.getEntities(player,5, EntitySelectors.exclude(player).and(new Predicate[Entity] {
        override def test(t: Entity): Boolean = t.isInstanceOf[EntityLiving]
      }))
    else new util.ArrayList[Entity]()

    for (ball <- balls) {
      var dest = newDest
      if (autoCount > 0 && !autoTarget.isEmpty) {
        val target = autoTarget.get(RandUtils.nextInt(autoTarget.size()))
        dest = new Vec3d(target.posX, target.posY+target.getEyeHeight, target.posZ)
        autoCount -= 1
      }

      val traceResult: MovingObjectPosition = Raytrace.perform(world, new Vec3d(ball.posX, ball.posY, ball.posZ),
        dest, basicSelector.and(EntitySelectors.exclude(player)))
      if (traceResult != null && traceResult.entityHit != null) {
        traceResult.entityHit.hurtResistantTime = -1
        MDDamageHelper.attack(ctx, traceResult.entityHit, getDamage(exp))
      }
      NetworkManager.sendSBEffectToClient(player,new Vec3d(ball.posX, ball.posY, ball.posZ)
        ,new Vec3d(dest.x,dest.y,dest.z))
      ball.setDead()
    }
    ctx.addSkillExp(0.001f * balls.size)
  }

  private def newDest: Vec3d = new Motion3D(player, 5, true).move(RAY_RANGE).getPosVec

  @SideOnly(Side.CLIENT)
  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def c_onEnd() = {
    import scala.collection.JavaConversions._
    for (ball <- balls) {
        ball.setDead()
    }
  }

}
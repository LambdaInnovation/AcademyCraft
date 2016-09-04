/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.meltdowner.skill

import java.util.function.Predicate

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{ClientRuntime, Context}
import cn.academy.core.client.ACRenderingHelper
import cn.academy.vanilla.meltdowner.entity.{EntityMdBall, EntityMdRaySmall}
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.VecUtils
import cn.lambdalib.util.helper.Motion3D
import cn.lambdalib.util.mc.{EntitySelectors, Raytrace}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.{DamageSource, MovingObjectPosition, Vec3}

/**
  * @author WeAthFolD, KSkun
  */
object ScatterBomb extends Skill("scatter_bomb", 2) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyID: Int) = activateSingleKey(rt, keyID, p => new SBContext(p))

}

object SBContext {

  final val MSG_SYNC_BALLS = "sync_balls"

}

import cn.lambdalib.util.generic.MathUtils._
import cn.academy.ability.api.AbilityAPIExt._
import SBContext._

class SBContext(p: EntityPlayer) extends Context(p, ScatterBomb) {

  private def getDamage(exp: Float) = lerpf(4, 6, exp)

  private val balls: java.util.List[EntityMdBall] = new java.util.ArrayList[EntityMdBall]
  private val basicSelector: Predicate[Entity] = EntitySelectors.everything
  private val MAX_TICKS: Int = 80
  private val MOD: Int = 10
  private val RAY_RANGE: Double = 15
  private var ticks: Int = 0
  private val exp: Float = ctx.getSkillExp

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
    val overload: Float = lerpf(185, 68, exp)
    if(!ctx.consume(overload, 0)) terminate()
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_onTick() = {
    ticks += 1

    if (ticks <= 80) {
      if (ticks >= 20 && ticks % MOD == 0) {
        val ball: EntityMdBall = new EntityMdBall(player)
        world.spawnEntityInWorld(ball)
        balls.add(ball)
        sendToClient(MSG_SYNC_BALLS, ball)
      }
      val cp: Float = lerpf(7, 9, exp)
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
    for (ball <- balls) {
      val dest: Vec3 = newDest
      val traceResult: MovingObjectPosition = Raytrace.perform(world, VecUtils.vec(ball.posX, ball.posY, ball.posZ),
        dest, basicSelector.and(EntitySelectors.exclude(player)))
      if (traceResult != null && traceResult.entityHit != null) {
        traceResult.entityHit.hurtResistantTime = -1
        MDDamageHelper.attack(ctx, traceResult.entityHit, getDamage(exp))
      }
      ball.setDead()
    }
    ctx.addSkillExp(0.001f * balls.size)
  }

  private def newDest: Vec3 = new Motion3D(player, 5, true).move(RAY_RANGE).getPosVec

  @SideOnly(Side.CLIENT)
  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def c_onEnd() = {
    val yoff: Double = if (ACRenderingHelper.isThePlayer(player)) 0 else 1.6
    import scala.collection.JavaConversions._
    for (ball <- balls) {
      // Spawn a ray for the ball
      val raySmall: EntityMdRaySmall = new EntityMdRaySmall(world)
      raySmall.viewOptimize = false
      val dest: Vec3 = newDest
      raySmall.setFromTo(ball.posX, ball.posY + yoff, ball.posZ, dest.xCoord, dest.yCoord, dest.zCoord)
      world.spawnEntityInWorld(raySmall)
    }
  }

}
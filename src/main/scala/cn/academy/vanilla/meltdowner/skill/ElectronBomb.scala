/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.meltdowner.skill

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.core.client.ACRenderingHelper
import cn.academy.vanilla.meltdowner.entity.{EntityMdBall, EntityMdRaySmall}
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.entityx.EntityCallback
import cn.lambdalib.util.generic.VecUtils
import cn.lambdalib.util.mc.{EntitySelectors, Raytrace}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.{MovingObjectPosition, Vec3}

/**
  * @author WeAthFolD, KSkun
  */
object ElectronBomb extends Skill("electron_bomb", 1) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyID: Int) = activateSingleKey(rt, keyID, p => new EBContext(p))

}

object EBContext {

  final val MSG_EFFECT = "effect"

}

import cn.lambdalib.util.generic.MathUtils._
import cn.academy.ability.api.AbilityAPIExt._
import EBContext._

class EBContext(p: EntityPlayer) extends Context(p, ElectronBomb) {

  private val LIFE: Int = 20
  private val LIFE_IMPROVED: Int = 5
  private val DISTANCE: Double = 15

  private def consume() = {
    val exp: Float = ctx.getSkillExp
    val overload: Float = lerpf(39, 17, exp)
    val cp: Float = lerpf(117, 135, exp)

    ctx.consume(overload, cp)
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  private def s_execute() = {
    if(consume()) {
      val exp: Float = ctx.getSkillExp

      val ball: EntityMdBall = new EntityMdBall(player, if (ctx.getSkillExp >= 0.8f) LIFE_IMPROVED
      else LIFE, new EntityCallback[EntityMdBall]() {
        def execute(ball: EntityMdBall) {
          val trace: MovingObjectPosition = Raytrace.perform(player.worldObj, VecUtils.vec(ball.posX, ball.posY, ball.posZ),
            getDest(player), EntitySelectors.exclude(player).and(EntitySelectors.of(classOf[EntityMdBall]).negate))
          if (trace != null && trace.entityHit != null) MDDamageHelper.attack(ctx, trace.entityHit, getDamage(exp))
          sendToClient(MSG_EFFECT, ball)
        }
      })
      player.worldObj.spawnEntityInWorld(ball)

      ctx.addSkillExp(.005f)
      ctx.setCooldown(lerpf(20, 10, exp).toInt)
    }
    terminate()
  }

  private def getDest(player: EntityPlayer): Vec3 = Raytrace.getLookingPos(player, DISTANCE).getLeft

  private def getDamage(exp: Float): Float = lerpf(12, 20, exp)

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[EBContext])
class EBContextC(par: EBContext) extends ClientContext(par) {

  private val DISTANCE: Double = 15
  
  @Listener(channel=MSG_EFFECT, side=Array(Side.CLIENT))
  private def c_spawnEffect(ball: EntityMdBall) = {
    val dest: Vec3 = getDest(player)
    val raySmall: EntityMdRaySmall = new EntityMdRaySmall(player.worldObj)
    raySmall.setFromTo(ball.posX, ball.posY + (if(ACRenderingHelper.isThePlayer(player)) 0
    else 1.6), ball.posZ, dest.xCoord, dest.yCoord, dest.zCoord)
    raySmall.viewOptimize = false
    player.worldObj.spawnEntityInWorld(raySmall)
  }

  private def getDest(player: EntityPlayer): Vec3 = Raytrace.getLookingPos(player, DISTANCE).getLeft
  
}
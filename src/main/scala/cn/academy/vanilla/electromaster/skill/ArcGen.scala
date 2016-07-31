/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.electromaster.skill

import cn.academy.ability.api.AbilityAPIExt._
import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{RegClientContext, ClientContext, Context, ClientRuntime}
import cn.academy.core.client.sound.ACSounds
import cn.academy.misc.achievements.ModuleAchievements
import cn.academy.vanilla.electromaster.client.effect.ArcPatterns
import cn.academy.vanilla.electromaster.entity.EntityArc
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.entityx.handlers.Life
import cn.lambdalib.util.generic.MathUtils._
import cn.lambdalib.util.generic.RandUtils
import cn.lambdalib.util.mc.{BlockSelectors, IBlockSelector, Raytrace}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.block.Block
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.{Items, Blocks}
import net.minecraft.item.ItemStack
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.world.World

/**
  * @author KSkun
  */
object ArcGen extends Skill("arc_gen", 1) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new ArcGenContext(p))

}

private object ArcGenContext {

  final val MSG_EFFECT = "effect"

  val blockFilter = new IBlockSelector {
    override def accepts(world: World, x: Int, y: Int, z: Int, block: Block) = {
      block == Blocks.water || block == Blocks.flowing_water ||
        BlockSelectors.filNormal.accepts(world, x, y, z, block)
    }
  }

}

import ArcGenContext._

class ArcGenContext(p: EntityPlayer) extends Context(p, ArcGen) {

  private val damage = lerpf(9, 15, ctx.getSkillExp)
  private val igniteProb = lerpf(0, 0.6f, ctx.getSkillExp)
  private val fishProb = if(ctx.getSkillExp > 0.5f) 0.1 else 0
  private val canStunEnemy = ctx.getSkillExp >= 1.0f
  private val range = lerpf(6, 15, ctx.getSkillExp)

  private def consume() = {
    val overload = lerpf(36, 16, ctx.getSkillExp)
    val cp = lerpf(117, 135, ctx.getSkillExp)

    ctx.consume(overload, cp)
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  private def s_perform() = {
    val world = player.worldObj
    // Perform ray trace
    val result = Raytrace.traceLiving(player, range, null, blockFilter)

    sendToClient(MSG_EFFECT, range.asInstanceOf[AnyRef])

    if(result != null) {
      var expincr = 0f
      if(result.typeOfHit == MovingObjectType.ENTITY) {
        EMDamageHelper.attack(ctx, result.entityHit, damage)
        expincr = getExpIncr(true)
      } else { //BLOCK
        val hx = result.blockX
        val hy = result.blockY
        val hz = result.blockZ
        val block = player.worldObj.getBlock(hx, hy, hz)
        if(block == Blocks.water) {
          if(RandUtils.ranged(0, 1) < fishProb) {
            world.spawnEntityInWorld(new EntityItem(
              world,
              result.hitVec.xCoord,
              result.hitVec.yCoord,
              result.hitVec.zCoord,
              new ItemStack(Items.cooked_fished)))
            ModuleAchievements.trigger(ctx.player, "electromaster.arc_gen")
          }
        } else {
          if(RandUtils.ranged(0, 1) < igniteProb) {
            if(world.getBlock(hx, hy + 1, hz) == Blocks.air) {
              world.setBlock(hx, hy + 1, hz, Blocks.fire, 0, 0x03)
            }
          }
        }
        expincr = getExpIncr(false)
      }
      consume()
      ctx.addSkillExp(expincr)
    }

    ctx.setCooldown(lerpf(40, 15, ctx.getSkillExp).toInt)
    terminate()
  }

  private def getExpIncr(effectiveHit : Boolean) = {
    if (effectiveHit) {
      lerpf(0.0048f, 0.0072f, ctx.getSkillExp)
    } else {
      lerpf(0.0018f, 0.0027f, ctx.getSkillExp)
    }
  }

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[ArcGenContext])
class ArcGenContextC(par: ArcGenContext) extends ClientContext(par) {

  @Listener(channel=MSG_EFFECT, side=Array(Side.CLIENT))
  private def c_spawnEffects(range: Float) = {
    val arc = new EntityArc(player, ArcPatterns.weakArc)
    arc.texWiggle = 0.7
    arc.showWiggle = 0.1
    arc.hideWiggle = 0.4
    arc.addMotionHandler(new Life(10))
    arc.lengthFixed = false
    arc.length = range

    player.worldObj.spawnEntityInWorld(arc)
    ACSounds.playClient(player, "em.arc_weak", 0.5f)
  }

}
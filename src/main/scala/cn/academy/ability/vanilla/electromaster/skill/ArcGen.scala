package cn.academy.ability.vanilla.electromaster.skill

import cn.academy.ability.Skill
import cn.academy.ability.api.AbilityAPIExt._
import cn.academy.ability.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.client.render.util.ArcPatterns
import cn.academy.client.sound.ACSounds
import cn.academy.entity.EntityArc
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util.{BlockSelectors, IBlockSelector, RandUtils, Raytrace}
import net.minecraft.block.Block
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.{Blocks, Items}
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import cn.lambdalib2.util.MathUtils.lerpf
import cn.lambdalib2.util.entityx.handlers.Life
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.{BlockPos, RayTraceResult}

/**
  * @author WeAthFolD, KSkun
  */
object ArcGen extends Skill("arc_gen", 1) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new ArcGenContext(p))

}

private object ArcGenContext {

  final val MSG_EFFECT = "effect"
  final val MSG_PERFORM = "perform"

  val blockFilter = new IBlockSelector {
    override def accepts(world: World, x: Int, y: Int, z: Int, block: Block) = {
      block == Blocks.WATER || block == Blocks.FLOWING_WATER ||
        BlockSelectors.filNormal.accepts(world, x, y, z, block)
    }
  }

}

import ArcGenContext._

class ArcGenContext(p: EntityPlayer) extends Context(p, ArcGen) {

  private val damage = lerpf(5, 9, ctx.getSkillExp)
  private val igniteProb = lerpf(0, 0.6f, ctx.getSkillExp)
  private val fishProb = if(ctx.getSkillExp > 0.5f) 0.1 else 0
  private val canStunEnemy = ctx.getSkillExp >= 1.0f
  private val range = lerpf(6, 15, ctx.getSkillExp)
  private val cp = lerpf(30, 70, ctx.getSkillExp)

  private def consume() = {
    val overload = lerpf(18, 11, ctx.getSkillExp)

    ctx.consume(overload, cp)
  }

  @Listener(channel=MSG_KEYDOWN, side=Array(Side.CLIENT))
  private def l_keydown() = {
    sendToServer(MSG_PERFORM)
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  private def s_perform() = {
    if(consume()) {
      val world = player.world
      // Perform ray trace
      val result = Raytrace.traceLiving(player, range, null, blockFilter)

      sendToClient(MSG_EFFECT, range.asInstanceOf[AnyRef])

      if (result != null) {
        var expincr = 0f
        if (result.typeOfHit == RayTraceResult.Type.ENTITY) {
          EMDamageHelper.attack(ctx, result.entityHit, damage)
          expincr = getExpIncr(true)
        } else {
          //BLOCK
          val hx = result.hitVec.x
          val hy = result.hitVec.y
          val hz = result.hitVec.z
          val pos = new BlockPos(hx, hy, hz)
          val block = player.world.getBlockState(pos).getBlock()
          if (block == Blocks.WATER) {
            if (RandUtils.ranged(0, 1) < fishProb) {
              world.spawnEntity(new EntityItem(
                world,
                result.hitVec.x,
                result.hitVec.y,
                result.hitVec.z,
                new ItemStack(Items.COOKED_FISH)))
              ArcGen.triggerAchievement(player)
            }
          } else {
            if (RandUtils.ranged(0, 1) < igniteProb) {
              val pos = new BlockPos(hx, hy + 1, hz)
              val state = world.getBlockState(pos)
              if (state.getBlock == Blocks.AIR) {

                world.setBlockState(pos, Blocks.FIRE.getDefaultState)
              }
            }
          }
          expincr = getExpIncr(false)
        }
        ctx.addSkillExp(expincr)
      }

      ctx.setCooldown(lerpf(15, 5, ctx.getSkillExp).toInt)
    }
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

    player.world.spawnEntity(arc)
    ACSounds.playClient(player, "em.arc_weak",SoundCategory.AMBIENT, 0.5f)
  }

}
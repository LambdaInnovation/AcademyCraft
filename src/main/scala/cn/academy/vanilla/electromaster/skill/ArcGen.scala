package cn.academy.vanilla.electromaster.skill

import cn.academy.ability.api.AbilityAPIExt._
import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.core.client.sound.ACSounds
import cn.academy.vanilla.electromaster.client.effect.ArcPatterns
import cn.academy.vanilla.electromaster.entity.EntityArc
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import net.minecraft.block.Block
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.{Blocks, Items}
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

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
      block == Blocks.water || block == Blocks.flowing_water ||
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
        if (result.typeOfHit == MovingObjectType.ENTITY) {
          EMDamageHelper.attack(ctx, result.entityHit, damage)
          expincr = getExpIncr(true)
        } else {
          //BLOCK
          val hx = result.blockX
          val hy = result.blockY
          val hz = result.blockZ
          val block = player.world.getBlock(hx, hy, hz)
          if (block == Blocks.water) {
            if (RandUtils.ranged(0, 1) < fishProb) {
              world.spawnEntityInWorld(new EntityItem(
                world,
                result.hitVec.x,
                result.hitVec.y,
                result.hitVec.z,
                new ItemStack(Items.cooked_fished)))
              ArcGen.triggerAchievement(player)
            }
          } else {
            if (RandUtils.ranged(0, 1) < igniteProb) {
              if (world.getBlock(hx, hy + 1, hz) == Blocks.air) {
                world.setBlock(hx, hy + 1, hz, Blocks.fire, 0, 0x03)
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

    player.world.spawnEntityInWorld(arc)
    ACSounds.playClient(player, "em.arc_weak", 0.5f)
  }

}
package cn.academy.vanilla.vecmanip.skill

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.KeyDelegate.DelegateState
import cn.academy.ability.api.context._
import cn.academy.core.client.sound.ACSounds
import cn.academy.core.util.Plotter
import cn.academy.vanilla.generic.client.effect.SmokeEffect
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.{Blocks, SoundEvents}
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

object Groundshock extends Skill("ground_shock", 1) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new GroundshockContext(p))

}

private object IVec {
  def apply(arr: Array[Int]) = new IVec(arr(0), arr(1), arr(2))
}

private case class IVec(x: Int, y: Int, z: Int)

private object GroundshockContext {

  final val MSG_PERFORM = "perform"

}

import GroundshockContext._
import collection.mutable

class GroundshockContext(p: EntityPlayer) extends Context(p, Groundshock) with IConsumptionProvider with IStateProvider {
  import cn.academy.ability.api.AbilityAPIExt._
  import scala.collection.JavaConversions._
  import cn.lambdalib2.util.MathUtils._
  var localTick = 0

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  def l_tick() = {
    localTick += 1

    val pitchDelta = localTick match {
      case t if t < 4 => t / 4.0f
      case t if t <= 20 => 1.0f
      case t if t <= 25 => 1.0f - (t - 20) / 5.0f
      case _ => 0.0f
    }

    player.rotationPitch -= pitchDelta * 0.2f
  }

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  def l_keyUp() = {
    if (localTick >= 5) {
      sendToServer(MSG_PERFORM)
    } else {
      terminate()
    }
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  def l_keyAbort() = {
    terminate()
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  def s_perform() = {
    if (player.onGround && consume()) {
      val planeLook = player.getLookVec.normalize()
      
      val plotter = new Plotter(player.posX.toInt, player.posY.toInt - 1,
        player.posZ.toInt, planeLook.x, 0, planeLook.z)

      var iter = maxIter

      var energy = initEnergy
      val dejavu_blocks = mutable.Set[IVec]()
      val dejavu_ent    = mutable.Set[Entity]()

      val rot = planeLook.copy()
      rot.rotateAroundY(90)

      val deltas = List((implicitly[Vec3d]((0.0, 0.0, 0.0)), 1.0), (rot, 0.7), (-rot, 0.7), (rot * 2, 0.3), (rot * -2, 0.3))

      val selector = EntitySelectors.living().and(EntitySelectors.exclude(player))

      def breakWithForce(x: Int, y: Int, z: Int, drop: Boolean) = {
        val blockPos = new BlockPos(x,y,z)
        val state = world.getBlockState(blockPos)
        val block = state.getBlock

        if (ctx.canBreakBlock(world, x, y, z)) {
          block.getBlockHardness(state,world,blockPos) match {
            case hardnessEnergy if hardnessEnergy >= 0 =>
              if (energy >= hardnessEnergy && block != Blocks.farmland && !block.getMaterial(state).isLiquid) {
                energy -= hardnessEnergy

                if (drop && RNG.nextFloat() < dropRate) {
                  block.dropBlockAsItemWithChance(world, blockPos, state, 1.0f, 0)
                }

                world.setBlockToAir(blockPos)
                world.playSound(x + 0.5, y + 0.5, z + 0.5, SoundEvents.BLOCK_ANVIL_DESTROY, SoundCategory.AMBIENT, .5f, 1f,false)
                //TODO This method seems to be empty.
              }
            case _ =>
          }
        }
      }

      while (energy > 0 && iter > 0) {
        val next = plotter.next()
        val (x, y, z) = (next(0), next(1), next(2))

        iter -= 1

        deltas.foreach { case (delta, prob) => {

          val pt = IVec((x + delta.x).floor.toInt, (y + delta.y).floor.toInt, (z + delta.z).floor.toInt)
          val block: Block = world.getBlock(pt.x, pt.y, pt.z)

          if (RNG.nextDouble() < prob) {
            if (block != Blocks.air &&
              !dejavu_blocks.contains(pt)) {
              dejavu_blocks += pt

              block match {
                case Blocks.stone =>
                  world.setBlock(pt.x, pt.y, pt.z, Blocks.cobblestone)
                  energy -= 0.4
                case Blocks.grass =>
                  world.setBlock(pt.x, pt.y, pt.z, Blocks.dirt)
                  energy -= 0.2
                case Blocks.farmland =>
                  energy -= 0.1
                case _ => energy -= 0.5
              }

              if (RNG.nextDouble() < groundBreakProb) {
                breakWithForce(x, y, z, false)
              }

              val aabb = AxisAlignedBB.getBoundingBox(pt.x-0.2, pt.y-0.2, pt.z-0.2, pt.x+1.4, pt.y+2.2, pt.z+1.4)
              val entities = WorldUtils.getEntities(world, aabb, selector)
              entities.foreach(entity => {
                if (!dejavu_ent.contains(entity)) {
                  dejavu_ent += entity
                  energy -= 1
                  ctx.attack(entity, damage)

                  entity.motionY = ySpeed
                  ctx.addSkillExp(0.002f)
                }
              })
            }
          }

          (1 to 3).foreach(d => breakWithForce(x, y + d, z, false))
        }}
      }

      energy = Double.MaxValue
      if (ctx.getSkillExp == 1) {
        val (x0, y0, z0) = (player.posX.toInt, player.posY.toInt, player.posZ.toInt)
        for {
          x <- x0 - 5 until x0 + 5
          y <- y0 - 1 until y0 + 1
          z <- z0 - 5 until z0 + 5
        } {
          implicit val block = world().getBlock(x, y, z)
          val hardness = block.getBlockHardness(world, x, y, z)
          if (hardness <= 0.6) {
            breakWithForce(x, y, z, true)
          }
        }
      }

      ctx.addSkillExp(0.001f)
      ctx.setCooldown(cooldown)
      Groundshock.triggerAchievement(player)
      sendToClient(MSG_PERFORM, dejavu_blocks.map(v => Array(v.x, v.y, v.z)).toArray)
    }
    terminate()
  }

  override def getConsumptionHint = consumption

  override def getState = if (localTick < 5) DelegateState.CHARGE else DelegateState.ACTIVE

  private val initEnergy: Double = lerpf(60, 120, ctx.getSkillExp)

  private val damage: Float = lerpf(4, 6, ctx.getSkillExp)

  private val consumption: Float = lerpf(80, 150, ctx.getSkillExp)

  private val overload: Float = lerpf(15, 10, ctx.getSkillExp)

  private val maxIter: Int = lerpf(10, 25, ctx.getSkillExp).toInt

  private val cooldown: Int = lerpf(80, 40, ctx.getSkillExp).toInt

  private val dropRate = lerpf(0.3f, 1.0f, ctx.getSkillExp)

  // y speed given to mobs.
  private val ySpeed: Float = rangef(0.6f, 0.9f) * lerpf(0.8f, 1.3f, ctx.getSkillExp)

  private[skill] def consume(): Boolean = ctx.consume(overload, consumption)

  private val groundBreakProb: Double = 0.3
}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[GroundshockContext])
class GroundshockContextC(par: GroundshockContext) extends ClientContext(par) {

  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  def c_perform(affectedBlocks: Array[Array[Int]]) = {
    if (isLocal) {
      par.consume()

      // Starts a coroutine that make player's look direction slash down.
      LIFMLGameEventDispatcher.INSTANCE.registerClientTick(new LIHandler[ClientTickEvent] {
        var ticks = 0

        override protected def onEvent(event: ClientTickEvent): Boolean = {
          ticks += 1
          player.rotationPitch += 3.4f

          if (ticks >= 4) { setDead() }

          true
        }
      })
    }

    ACSounds.playClient(player, "vecmanip.groundshock", 2)

    affectedBlocks.map(arr => IVec(arr)).foreach(pt => {
      for (i <- 0 until rangei(4, 8)) {
        def randvel() = ranged(-0.2, 0.2)
        val entity = new EntityDiggingFX(
          world,
          pt.x + nextDouble(), pt.y + 1 + nextDouble() * 0.5 + 0.2, pt.z + nextDouble(),
          randvel(), 0.1 + nextDouble() * 0.2, randvel(),
          world.getBlock(pt.x, pt.y, pt.z),
          ForgeDirection.UP.ordinal())

        Minecraft.getMinecraft.effectRenderer.addEffect(entity)
      }

      if (nextFloat() < 0.5f) {
        val eff = new SmokeEffect(world)
        val pos = (pt.x + 0.5 + ranged(-.3, .3), pt.y + 1 + ranged(0, 0.2), pt.z + 0.5 + ranged(-.3, .3))
        val vel = (ranged(-.03, .03), ranged(.03, .06), ranged(-.03, .03))

        eff.setPos(pos)
        eff.setVel(vel)
        world.spawnEntityInWorld(eff)
      }
    })
  }

}
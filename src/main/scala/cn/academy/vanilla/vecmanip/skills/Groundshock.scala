package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.KeyDelegate.DelegateState
import cn.academy.ability.api.context.{IStateProvider, IConsumptionProvider, ClientRuntime, Context}
import cn.academy.core.client.sound.ACSounds
import cn.academy.core.util.Plotter
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.RegInitCallback
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.s11n.network.NetworkS11n
import cn.lambdalib.util.generic.RandUtils
import cn.lambdalib.util.mc.{EntitySelectors, WorldUtils, Vec3}
import cpw.mods.fml.relauncher.{SideOnly, Side}
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.particle.EntityDiggingFX
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.util.{MathHelper, DamageSource, AxisAlignedBB, Vec3}
import net.minecraftforge.common.util.ForgeDirection

@Registrant
object Groundshock extends Skill("ground_shock", 1) {

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
import cn.lambdalib.util.mc.MCExtender._
import collection.mutable
import RandUtils._

class GroundshockContext(p: EntityPlayer) extends Context(p) with IConsumptionProvider with IStateProvider {
  import cn.academy.ability.api.AbilityAPIExt._
  import scala.collection.JavaConversions._
  import cn.lambdalib.util.generic.MathUtils._

  implicit val skill_ = Groundshock
  implicit val adata_ = aData()

  var localTick = 0

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  def l_tick() = {
    localTick += 1
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

  @SideOnly(Side.CLIENT)
  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  def c_perform(affectedBlocks: Array[Array[Int]]) = {
    if (isLocal) {
      consume()
      addSkillCooldown(cooldown)
    }

    ACSounds.playClient(player, "vecmanip.groundshock", 2)

    affectedBlocks.map(arr => IVec(arr)).foreach(pt => {
      for (i <- 0 until rangei(1, 4)) {
        def randvel() = ranged(-0.2, 0.2)
        val entity = new EntityDiggingFX(
          world,
          pt.x + nextDouble(), pt.y + 1 + nextDouble() * 0.5 + 0.2, pt.z + nextDouble(),
          randvel(), 0.1 + nextDouble() * 0.2, randvel(),
          world.getBlock(pt.x, pt.y, pt.z),
          ForgeDirection.UP.ordinal())

        Minecraft.getMinecraft.effectRenderer.addEffect(entity)
      }
    })
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

      val deltas = List((implicitly[Vec3]((0.0, 0.0, 0.0)), 1.0), (rot, 0.7), (-rot, 0.7), (rot * 2, 0.3), (rot * -2, 0.3))

      val selector = EntitySelectors.and(EntitySelectors.living, EntitySelectors.excludeOf(player))

      def breakWithForce(x: Int, y: Int, z: Int, drop: Boolean)(implicit block: Block) = {
        val hardnessEnergy = math.max(0, block.getBlockHardness(world, x, y, z))
        if (energy >= hardnessEnergy && block != Blocks.farmland && !block.getMaterial.isLiquid) {
          energy -= hardnessEnergy

          if (drop && RNG.nextFloat() < dropRate) {
            block.dropBlockAsItemWithChance(world, x, y, z, world.getBlockMetadata(x, y, z), 1.0f, 0)
          }

          world.setBlock(x, y, z, Blocks.air)
          world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, block.stepSound.getBreakSound, .5f, 1f)
        }
      }

      while (energy > 0 && iter > 0) {
        val next = plotter.next()
        val (x, y, z) = (next(0), next(1), next(2))

        iter -= 1

        deltas.foreach { case (delta, prob) => {

          val pt = IVec((x + delta.x).toInt, (y + delta.y).toInt, (z + delta.z).toInt)
          val block: Block = world.getBlock(pt.x, pt.y, pt.z)
          implicit val block_ = block

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
                  entity.attackEntityFrom(DamageSource.causePlayerDamage(player), damage)
                  entity.motionY = ySpeed
                  addSkillExp(0.002f)
                }
              })
            }
          }

          (1 to 3).foreach(d => breakWithForce(x, y + d, z, false))
        }}
      }

      energy = Double.MaxValue
      if (skillExp == 1) {
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

      addSkillExp(0.001f)
      sendToClient(MSG_PERFORM, dejavu_blocks.map(v => Array(v.x, v.y, v.z)).toArray)
    }
    terminate()
  }

  override def getConsumptionHint = consumption

  override def getState = if (localTick < 5) DelegateState.CHARGE else DelegateState.ACTIVE

  private def initEnergy: Double = lerpf(60, 120, skillExp)

  private def damage: Float = lerpf(7, 16, skillExp)

  private def consumption: Float = lerpf(300, 180, skillExp)

  private def overload: Float = lerpf(135, 100, skillExp)

  private def maxIter: Int = lerpf(10, 25, skillExp).toInt

  // y speed given to mobs.
  private def ySpeed: Float = rangef(1.0f, 1.2f) * lerpf(0.8f, 1.3f, skillExp)

  private def consume(): Boolean = cpData.perform(overload, consumption)

  private def groundBreakProb: Double = 0.3

  private def cooldown: Int = lerpf(45, 20, skillExp).toInt

  private def dropRate = lerpf(0.3f, 1.0f, skillExp)

}
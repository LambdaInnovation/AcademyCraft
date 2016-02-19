package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{ClientRuntime, Context}
import cn.academy.core.util.Plotter
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.RegInitCallback
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.s11n.network.NetworkS11n
import cn.lambdalib.util.generic.RandUtils
import cn.lambdalib.util.mc.{EntitySelectors, WorldUtils, Vec3}
import cpw.mods.fml.relauncher.{SideOnly, Side}
import net.minecraft.client.Minecraft
import net.minecraft.client.particle.EntityDiggingFX
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.util.{DamageSource, AxisAlignedBB, Vec3}
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

class GroundshockContext(p: EntityPlayer) extends Context(p) {
  import cn.academy.ability.api.AbilityAPIExt._
  import scala.collection.JavaConversions._

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  def l_keyUp() = {
    sendToServer(MSG_PERFORM)
  }

  @SideOnly(Side.CLIENT)
  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  def c_perform(affectedBlocks: Array[Array[Int]]) = {
    if (isLocal) {
      consume()
      terminate()
    }

    affectedBlocks.map(arr => IVec(arr)).foreach(pt => {
      for (i <- 0 until rangei(1, 4)) {
        def randvel() = ranged(-0.2, 0.2)
        val entity = new EntityDiggingFX(
          world,
          pt.x + nextDouble(), pt.y + nextDouble() * 0.5 + 0.2, pt.z + nextDouble(),
          randvel(), 0.1 + nextDouble() * 0.2, randvel(),
          world.getBlock(pt.x, pt.y, pt.z),
          ForgeDirection.UP.ordinal())

        Minecraft.getMinecraft.effectRenderer.addEffect(entity)
      }
    })
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  def s_perform() = {
    def cvtpos(cvt: Double => Int) = Array(cvt(player.posX), player.posY.toInt - 1, cvt(player.posZ))

    if (player.onGround && consume()) {

      val begin0 = cvtpos(_.toInt)
      val begin1 = cvtpos(math.ceil(_).toInt)
      val look = player.getLookVec
      look.yCoord = 0
      val planeLook = look.normalize()
      val plotter0 = new Plotter(begin0(0), begin0(1), begin0(2), planeLook.x, 0, planeLook.z)
      val plotter1 = new Plotter(begin1(0), begin1(1), begin1(2), planeLook.x, 0, planeLook.z)

      var energy = initEnergy
      val dejavu = mutable.Set[IVec]()
      val dejavuEnt = mutable.Set[Entity]()
      while (energy > 0) {
        def process(plotter: Plotter) = {
          val pt = IVec(plotter.next())
          if (!dejavu.contains(pt)) {
            dejavu += pt
            val block = world.getBlock(pt.x, pt.y, pt.z)
            block match {
              case Blocks.stone =>
                world.setBlock(pt.x, pt.y, pt.z, Blocks.cobblestone)
                energy -= 0.4
              case Blocks.grass =>
                world.setBlock(pt.x, pt.y, pt.z, Blocks.dirt)
                energy -= 0.2
              case _ => energy -= 0.5
            }
            val hardness = block.getBlockHardness(world, pt.x, pt.y, pt.z)
            if (RandUtils.nextDouble() < groundBreakProb && energy > hardness*0.4) {
              energy -= hardness*0.4
              world.setBlock(pt.x, pt.y, pt.z, Blocks.air)
            }

            val upper = world.getBlock(pt.x, pt.y + 1, pt.z)
            if (upper.isReplaceable(world, pt.x, pt.y + 1, pt.z)) {
              world.setBlock(pt.x, pt.y + 1, pt.z, Blocks.air)
            }

            val aabb = AxisAlignedBB.getBoundingBox(pt.x-0.2, pt.y-0.2, pt.z-0.2, pt.x+1.4, pt.y+2.2, pt.z+1.4)
            val entities = WorldUtils.getEntities(world, aabb, EntitySelectors.living)
            entities.foreach(entity => {
              if (!dejavuEnt.contains(entity)) {
                dejavuEnt += entity
                energy -= 1
                entity.attackEntityFrom(DamageSource.causePlayerDamage(player), damage)
              }
            })
          }
        }

        process(plotter0)
        process(plotter1)

        sendToClient(MSG_PERFORM, dejavu.map(v => Array(v.x, v.y, v.z)).toArray)
      }
    }


  }

  private def initEnergy: Double = 5

  private def damage: Float = 5

  private def consume(): Boolean = true

  private def groundBreakProb: Double = 0.3

}
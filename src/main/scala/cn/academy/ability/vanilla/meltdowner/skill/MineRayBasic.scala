package cn.academy.ability.vanilla.meltdowner.skill

import cn.academy.ability.api.context.{ClientRuntime, RegClientContext}
import cn.academy.entity.EntityMineRayBasic
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.world.World

/**
  * @author WeAthFolD, KSkun
  */
object MineRayBasic extends MineRaysBase("basic", 3) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyID: Int) = activateSingleKey(rt, keyID, p => new BasicMRContext(p))

}

class BasicMRContext(p: EntityPlayer) extends MRContext(p, MineRayBasic) {

  setRange(10)
  setHarvestLevel(2)
  setSpeed(0.2f, 0.4f)
  setConsumption(12.0f, 7.0f)
  setOverload(200f, 150f)
  setCooldown(40f, 20f)
  setExpIncr(0.0005f)

  override protected def onBlockBreak(world: World, x: Int, y: Int, z: Int, block: Block) = {
    world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, block.stepSound.getBreakSound, .5f, 1f)
    block.dropBlockAsItemWithChance(world, x, y, z, world.getBlockMetadata(x, y, z), 1.0f, 0)
    world.setBlock(x, y, z, Blocks.air)
  }

}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[BasicMRContext])
class BasicMRContextC(par: BasicMRContext) extends MRContextC(par) {

  override protected def createRay: Entity = new EntityMineRayBasic(player)

}
package cn.academy.ability.vanilla.meltdowner.skill

import cn.academy.ability.context.{ClientContext, ClientRuntime, RegClientContext}
import cn.academy.entity.EntityMineRayExpert
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.world.World

/**
  * @author WeAthFolD, KSkun
  */
object MineRayExpert extends MineRaysBase("expert", 4) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyID: Int) = activateSingleKey(rt, keyID, p => new ExpertMRContext(p))

}

class ExpertMRContext(p: EntityPlayer) extends MRContext(p, MineRayExpert) {

  setRange(20)
  setHarvestLevel(5)
  setSpeed(0.5f, 1f)
  setConsumption(25f, 15f)
  setOverload(300f, 200f)
  setCooldown(60f, 30f)
  setExpIncr(0.0003f)

  override protected def onBlockBreak(world: World, x: Int, y: Int, z: Int, block: Block) = {
    world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, block.stepSound.getBreakSound, .5f, 1f)
    block.dropBlockAsItemWithChance(world, x, y, z, world.getBlockMetadata(x, y, z), 1.0f, 0)
    world.setBlock(x, y, z, Blocks.air)
  }

}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[ExpertMRContext])
class ExpertMRContextC(par: ExpertMRContext) extends MRContextC(par) {

  override protected def createRay: Entity = new EntityMineRayExpert(player)

}
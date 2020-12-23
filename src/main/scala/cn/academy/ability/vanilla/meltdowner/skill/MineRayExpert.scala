package cn.academy.ability.vanilla.meltdowner.skill

import cn.academy.ability.context.{ClientContext, ClientRuntime, RegClientContext}
import cn.academy.entity.EntityMineRayExpert
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
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

  override protected def onBlockBreak(world: World, pos: BlockPos, block: Block) = {
    val st = world.getBlockState(pos)
    val eff = block.getSoundType(st, world, pos, player).getBreakSound
    world.playSound(x + 0.5, y + 0.5, z + 0.5, eff, SoundCategory.BLOCKS, .5f, 1f, false)
    block.dropBlockAsItemWithChance(world, pos, world.getBlockState(pos), 1.0f, 0)
    world.setBlockState(pos, Blocks.AIR.getDefaultState)
  }

}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[ExpertMRContext])
class ExpertMRContextC(par: ExpertMRContext) extends MRContextC(par) {

  override protected def createRay: Entity = new EntityMineRayExpert(player)

}
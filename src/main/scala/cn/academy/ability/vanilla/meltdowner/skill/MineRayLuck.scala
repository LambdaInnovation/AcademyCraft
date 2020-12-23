package cn.academy.ability.vanilla.meltdowner.skill

import cn.academy.ability.context.{ClientRuntime, RegClientContext}
import cn.academy.entity.EntityMineRayLuck
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
object MineRayLuck extends MineRaysBase("luck", 5) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyID: Int) = activateSingleKey(rt, keyID, p => new LuckMRContext(p))

}

class LuckMRContext(p: EntityPlayer) extends MRContext(p, MineRayLuck) {

  setRange(20)
  setHarvestLevel(5)
  setSpeed(.5f, 1)
  setConsumption(50, 35)
  setOverload(350, 300)
  setCooldown(60, 30)
  setExpIncr(0.0003f)

  override protected def onBlockBreak(world: World, pos: BlockPos, block: Block) = {
    val st = world.getBlockState(pos)
    val snd = block.getSoundType(st, world, pos, p).getBreakSound
    world.playSound(x + 0.5, y + 0.5, z + 0.5, snd, SoundCategory.BLOCKS, .5f, 1f, false)
    block.dropBlockAsItemWithChance(world, pos, world.getBlockState(pos), 1.0f, 3)
    world.setBlockState(pos, Blocks.AIR.getDefaultState)
  }

}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[LuckMRContext])
class LuckMRContextC(par: LuckMRContext) extends MRContextC(par) {

  override protected def createRay: Entity = new EntityMineRayLuck(player)

}
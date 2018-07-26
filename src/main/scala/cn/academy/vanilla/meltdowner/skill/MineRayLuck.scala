/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.meltdowner.skill

import cn.academy.ability.api.context.{ClientRuntime, RegClientContext}
import cn.academy.vanilla.meltdowner.entity.EntityMineRayLuck
import cn.lambdalib2.annoreg.core.Registrant
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
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

  override protected def onBlockBreak(world: World, x: Int, y: Int, z: Int, block: Block) = {
    world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, block.stepSound.getBreakSound, .5f, 1f)
    block.dropBlockAsItemWithChance(world, x, y, z, world.getBlockMetadata(x, y, z), 1.0f, 3)
    world.setBlock(x, y, z, Blocks.air)
  }

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[LuckMRContext])
class LuckMRContextC(par: LuckMRContext) extends MRContextC(par) {

  override protected def createRay: Entity = new EntityMineRayLuck(player)

}

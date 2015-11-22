/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.vanilla.meltdowner.skill;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import cn.academy.vanilla.meltdowner.entity.EntityMineRayExpert;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
public class MineRayExpert extends MineRaysBase {

	public static final MineRayExpert instance = new MineRayExpert();

	private MineRayExpert() {
		super("expert", 4);
	}

	@Override
	protected void onBlockBreak(World world, int x, int y, int z, Block block) {
		world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, block.stepSound.getBreakSound(), .5f, 1f);
		block.dropBlockAsItemWithChance(world, x, y, z, world.getBlockMetadata(x, y, z), 1.0f, 0);
		world.setBlock(x, y, z, Blocks.air);
	}

	@SideOnly(Side.CLIENT)
	@Override
	protected Entity createRay(EntityPlayer player) {
		return new EntityMineRayExpert(player);
	}

}

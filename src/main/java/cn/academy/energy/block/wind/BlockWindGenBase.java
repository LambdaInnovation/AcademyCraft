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
package cn.academy.energy.block.wind;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.core.block.ACBlockMulti;

/**
 * @author WeAthFolD
 */
public class BlockWindGenBase extends ACBlockMulti {

	public BlockWindGenBase() {
		super("windgen_base", Material.rock);
		setHardness(4.0f);
		setHarvestLevel("pickaxe", 2);
		addSubBlock(new int[][] {
			{ 0, 1, 0 }
		});
		finishInit();
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileWindGenBase();
	}

	@Override
	public double[] getRotCenter() {
		return new double[] { 0.5, 0, 0.5 };
	}
	


}

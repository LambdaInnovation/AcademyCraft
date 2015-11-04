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
package cn.academy.core.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.lambdalib.multiblock.BlockMulti;
import cn.liutils.util.mc.StackUtils;

/**
 * @author WeAthFolD
 */
public abstract class ACBlockMulti extends BlockMulti {

	public ACBlockMulti(String name, Material mat) {
		super(mat);
		setCreativeTab(AcademyCraft.cct);
		setBlockName("ac_" + name);
		setBlockTextureName("academy:" + name);
	}

	@Override
    public void breakBlock(World world, int x, int y, int z, Block block, int wtf) {
		if(!world.isRemote) {
			TileEntity te = world.getTileEntity(x, y, z);
			if(te instanceof IInventory) {
				StackUtils.dropItems(world, x, y, z, (IInventory) te);
			}
		}
		super.breakBlock(world, x, y, z, block, wtf);
    }
	
}

/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.misc.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;


/**
 * 各种手残啊魂淡
 * @author Lyt99
 */
public class ACBlockContainer extends BlockContainer{

	protected ACBlockContainer(Material arg0) {
		super(arg0);
	}

	@Override
	public TileEntity createNewTileEntity(World arg0, int arg1) {
		return null;
	}
	
	public void dropItems(World world,int x,int y,int z,ItemStack stack){
        EntityItem entityItem = new EntityItem(world,x, y, z,stack);

        if (stack.hasTagCompound()) 
        	entityItem.getEntityItem().setTagCompound(
        			(NBTTagCompound) stack.getTagCompound().copy());

        world.spawnEntityInWorld(entityItem);
	}

}

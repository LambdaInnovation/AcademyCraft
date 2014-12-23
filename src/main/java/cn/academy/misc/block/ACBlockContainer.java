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

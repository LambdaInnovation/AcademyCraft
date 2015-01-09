/**
 * Copyright (C) Lambda-Innovation, 2013-2014
 * This code is open-source. Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 */
package cn.academy.misc.item;

import cn.academy.core.AcademyCraft;
import cn.academy.core.register.ACBlocks;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 *
 */
public class ItemModuleAttached extends Item {

	int attachID = 0;

	public ItemModuleAttached() {
		setCreativeTab(AcademyCraft.cct);
		setUnlocalizedName("ad_card");
		setTextureName("academy:card");
	}
	
/*	@Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world,
    		int x, int y, int z, int meta, float a, float b, float c)
    {
    	Block block = world.getBlock(x, y, z);
    	if(block != ACBlocks.ability_developer)
    		return false;
    	if(!player.worldObj.isRemote) {
    		TileAbilityDeveloper dev = (TileAbilityDeveloper) world.getTileEntity(x, y, z);
    		if(dev.insertAttachedModule(attachID)) {
    			player.inventory.decrStackSize(player.inventory.currentItem, 1);
    			return true;
    		}
    	}
        return false;
    }*/

}

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

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import cn.academy.ability.electro.IShootable;
import cn.academy.core.AcademyCraftMod;
import cn.academy.misc.entity.EntityThrowingCoin;
import cn.liutils.api.util.GenericUtils;

/**
 * The coin from the game genter which is used by Misaka Mikoto for her prouding Railgun skill!
 * ~\(≧▽≦)/~
 * @author KSkun, WeAthFolD
 */
public class ItemCoin extends Item implements IShootable {
	
	public static final int THROWING_TIME = 40;
	
	public ItemCoin() {
		setUnlocalizedName("ac_coin");
		setTextureName("academy:coin-front");
		setCreativeTab(AcademyCraftMod.cct);
	}
	
    @Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean equipped) {
    	NBTTagCompound nbt = GenericUtils.loadCompound(stack);
    	if(!(entity instanceof EntityPlayer) || !equipped) {
    		reset(nbt);
    		return;
    	}
    	if(!nbt.getBoolean("throwing"))
    		return;
    	int prog = nbt.getInteger("prog") + 1;
    	//System.out.println("prg: " + prog);
    	if(prog > THROWING_TIME) {
    		reset(nbt);
    		return;
    	}
    	((EntityPlayer)entity).isSwingInProgress = false;
    	nbt.setInteger("prog", prog);
    }

    @Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
    {
    	NBTTagCompound nbt = GenericUtils.loadCompound(stack);
    	if(nbt.getBoolean("throwing")) return stack;
    	nbt.setBoolean("throwing", true);
    	nbt.setInteger("prog", 0);
    	
    	nbt.setLong("startTime", GenericUtils.getSystemTime());
    	if(world.isRemote)
    		world.spawnEntityInWorld(new EntityThrowingCoin(player, stack));
        return stack;
    }

	@Override
	public boolean inProgress(ItemStack stack) {
		return GenericUtils.loadCompound(stack).getBoolean("throwing");
	}

	@Override
	public double getProgress(ItemStack stack) {
		return (double)GenericUtils.loadCompound(stack).getInteger("prog") / THROWING_TIME;
	}
	
	private void reset(NBTTagCompound nbt) {
		nbt.setBoolean("throwing", false);
    	nbt.setInteger("prog", 0);
	}

	@Override
	public boolean isAcceptable(double prog) {
		return prog >= 0.75 && prog <= 0.95;
	}

}

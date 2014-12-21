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

import cn.academy.core.AcademyCraftMod;
import cn.liutils.api.util.GenericUtils;
import cn.liutils.api.util.Pair;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * @author KSkun
 * 硬币什么的功能之后再加吧
 */
public class ItemCoin extends Item /*implements IRailgunQTE*/ {
	
	public static final int THROWING_TIME = 40;
	
	public ItemCoin() {
		setUnlocalizedName("ac_coin");
		setTextureName("academy:coin-front");
		setCreativeTab(AcademyCraftMod.cct);
	}
	
    @Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int par4, boolean par5) {
    	NBTTagCompound nbt = GenericUtils.loadCompound(stack);
    	if(!par5) {
    		nbt.setBoolean("isThrowing", false);
    		return;
    	}
    	
    	boolean b = nbt.getBoolean("isThrowing");
    	if(b) {
    		((EntityPlayer)entity).isSwingInProgress = false;
    		int ticks = nbt.getInteger("throwTick");
    		if(++ticks >= THROWING_TIME) {
    			nbt.setBoolean("isThrowing", false);
    			nbt.setInteger("throwTick", 0);
    		} else nbt.setInteger("throwTick", ticks);
    	}
    }


    @Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
    {
    	NBTTagCompound nbt = GenericUtils.loadCompound(stack);
    	if(nbt.getBoolean("isThrowing")) return stack;
    	
    	nbt.setBoolean("isThrowing", true);
		nbt.setInteger("throwTick", 0);
        return stack;
    }

/*	@Override
	public boolean isQTEinProgress(ItemStack stack) {
		NBTTagCompound nbt = GenericUtils.loadCompound(stack);
		return nbt.getBoolean("isThrowing");
	}

	@Override
	public float getQTEProgress(ItemStack stack) {
		NBTTagCompound nbt = GenericUtils.loadCompound(stack);
		return ((float)nbt.getInteger("throwTick")) / THROWING_TIME;
	}

	Pair<Float, Float> range = new Pair<Float, Float>(0.7F, 0.9F);
	@Override
	public Pair<Float, Float> getAcceptedRange() {
		return range;
	}*/

}

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
import cn.liutils.util.GenericUtils;

/**
 * The coin from the game center which is used by Misaka Mikoto for her proud Railgun skill!
 * ~\(≧▽≦)/~
 * @author KSkun, WeAthFolD
 */
public class ItemCoin extends Item implements IShootable {
	
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
    	EntityThrowingCoin etc = getThrowingEntity(world, stack);
    	if(etc == null || etc.isDead) {
    		reset(nbt);
    		return;
    	}
    	((EntityPlayer)entity).isSwingInProgress = false;
    }

    @Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
    {
    	NBTTagCompound nbt = GenericUtils.loadCompound(stack);
    	if(nbt.getBoolean("throwing")) return stack;
    	//System.out.println("Spawn");
    	EntityThrowingCoin etc = world.isRemote ? 
    		new EntityThrowingCoin.AvoidSync(player, stack) :
    		new EntityThrowingCoin(player, stack);
    	world.spawnEntityInWorld(etc);
    	nbt.setInteger("entID", etc.getEntityId());
    	nbt.setBoolean("throwing", true);
        return stack;
    }

	private void reset(NBTTagCompound nbt) {
		nbt.setBoolean("throwing", false);
	}
	
	private EntityThrowingCoin getThrowingEntity(World world, ItemStack is) {
		NBTTagCompound nbt = is.getTagCompound();
		Entity e = world.getEntityByID(nbt.getInteger("entID"));
		if(e == null || !(e instanceof EntityThrowingCoin))
			return null;
		return (EntityThrowingCoin) e;
	}
	
	@Override
	public boolean inProgress(EntityPlayer ep, ItemStack stack) {
		return GenericUtils.loadCompound(stack).getBoolean("throwing");
	}
	
	@Override
	public double getProgress(EntityPlayer ep, ItemStack stack) {
		EntityThrowingCoin etc = getThrowingEntity(ep.worldObj, stack);
		return etc == null ? 0.0 : etc.getProgress();
	}

	@Override
	public boolean isAcceptable(double prog) {
		return prog >= 0.75 && prog <= 0.95;
	}

}

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
import cn.annoreg.core.ctor.Constructible;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;

/**
 * @author KSkun
 * 狼狼记得重写吃药的动作，我们要每天萌萌哒
 */
public class ItemCapsule extends Item {
	
	private static String[] uname = {"", "ability_capsule1", "ability_capsule2", "ability_capsule3"};
	
	int capsuleID;
	
	/**
	 * 
	 * @param subID
	 * metadata
	 * TODO START FROM 1!
	 * 
	 * @param capsuleID
	 * metadata2
	 */
	@Constructible
	public ItemCapsule(int subID) {
		setCreativeTab(AcademyCraftMod.cct);
		capsuleID = subID;
		this.setUnlocalizedName(uname[subID]);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister par1IconRegister) {
		this.itemIcon = par1IconRegister.registerIcon("academy:capsule" + capsuleID);
	}
	
}

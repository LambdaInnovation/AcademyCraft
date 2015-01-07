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

import java.util.List;

import cn.academy.core.AcademyCraftMod;
import cn.annoreg.core.ctor.Constructible;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

/**
 * @author KSkun
 * AcademyCraft Record
 */
public class ACRecord extends ItemRecord {
	
	private static final String[] rnames = {"ac1", "ac2", "ac3"};
	private static final String[] unames = {"ac_record1", "ac_record2", "ac_record3"};
	
	@Constructible
	public ACRecord(int subID) {
		this(rnames[subID], subID);
		this.setUnlocalizedName(unames[subID]);
	}
	
	private int recID;

	/**
	 * 
	 * @param rname
	 * 				Record's Name
	 * 
	 * @param subID
	 * 				Record's ID
	 */
	public ACRecord(String rname, int subID) {
		super(rname);
		setUnlocalizedName("ACRecord");
		setCreativeTab(AcademyCraftMod.cct);
		recID = subID;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamage(int par1) {
		return this.itemIcon;
		
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public EnumRarity getRarity(ItemStack par1ItemStack) {
		return EnumRarity.rare;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack par1ItemStack,
			EntityPlayer par2EntityPlayer, List par3List, boolean par4) {
		String des[] = new String[] {"Only My Railgun by FripSide", 
				"Sister's Noise by FripSide",
				"LEVEL5 -Judgelight- by FripSide"};
		par3List.add(des[recID]);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public String getRecordNameLocal() {
		String rname[] = new String[] {"FripSide - Only My Railgun",
				"FripSide - Sister's Noise",
				"FripSide - LEVEL5 -Judgelight-"};
		return rname[recID];
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item par1, CreativeTabs par2CreativeTabs,
			List par3List) {
		super.getSubItems(par1, par2CreativeTabs, par3List);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister par1IconRegister) {
		this.itemIcon = par1IconRegister.registerIcon("academy:record" + recID);
	}
	
	private static final ResourceLocation sources[] = new ResourceLocation[] {
		new ResourceLocation("academy:records.omr"),
		new ResourceLocation("academy:records.sn"),
		new ResourceLocation("academy:records.lv5")
	};
	
	@Override
	@SideOnly(Side.CLIENT)
	public ResourceLocation getRecordResource(String par1) {
		return sources[recID];
	}
	
}

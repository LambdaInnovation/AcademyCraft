/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.misc.item;

import java.util.List;

import cn.academy.core.AcademyCraft;
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
 * AcademyCraft Records
 */
public class ACRecord extends ItemRecord {
	
	private static final String[] rnames = {"ac1", "ac2", "ac3"};
	private static final String[] unames = {"ac_record1", "ac_record2", "ac_record3"};
	private static final ResourceLocation sources[] = new ResourceLocation[] {
		new ResourceLocation("academy:records.omr"),
		new ResourceLocation("academy:records.sn"),
		new ResourceLocation("academy:records.lv5")
	};
	
	private int recId;
	
	public ACRecord(int subId) {
		this(rnames[subId]);
		this.setUnlocalizedName(unames[subId]);
		setCreativeTab(AcademyCraft.cct);
		recId = subId;
	}
	
	public ACRecord(String rname) {
		super(rname);
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
		String des[] = new String[] {"Only My Railgun by fripSide", 
				"Sister's Noise by fripSide",
				"LEVEL5 -Judgelight- by fripSide"};
		par3List.add(des[recId]);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public String getRecordNameLocal() {
		String rname[] = new String[] {"fripSide - Only My Railgun",
				"fripSide - Sister's Noise",
				"fripSide - LEVEL5 -Judgelight-"};
		return rname[recId];
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
		this.itemIcon = par1IconRegister.registerIcon("academy:record" + recId);
	}

	@Override
	public ResourceLocation getRecordResource(String par1) {
		return sources[recId];
	}
	
}

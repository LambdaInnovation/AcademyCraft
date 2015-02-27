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
package cn.academy.energy.item;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import cn.academy.core.AcademyCraft;
import cn.academy.energy.util.EnergyUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 能量水晶
 * @author Lyt99
 */
public class ItemEnergyCrystal extends Item implements IElectricItem {
			
	protected IIcon[] textures;
	protected int maxCharge = 500000, tier = 2, transferLimit = 128;

	public ItemEnergyCrystal(){
		setUnlocalizedName("ac_energycrystal");
		setCreativeTab(AcademyCraft.cct);
		setMaxDamage(27);
		setMaxStackSize(1);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister iconRegister){
		this.textures = new IIcon[2];
		this.textures[0] = iconRegister.registerIcon("academy:energycrystal_empty");
		this.textures[1] = iconRegister.registerIcon("academy:energycrystal");
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamage(int charge){
		if(charge == 0)
			return this.textures[0];
		else
			return this.textures[1];
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs par2CreativeTabs, List par3List){
		ItemStack charged = new ItemStack(this, 1);
		ItemStack discharged = new ItemStack(this, 1);
		EnergyUtils.tryCharge(charged, Integer.MAX_VALUE);
		par3List.add(discharged);
		par3List.add(charged);
	}
	
	@Override
	public void addInformation(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4){
		par3List.add(ElectricItem.manager.getToolTip(par1ItemStack));
	}

//IC2 API INTERFACE
	@Override
	public boolean canProvideEnergy(ItemStack itemStack) {
		return true;
	}

	@Override
	public Item getChargedItem(ItemStack itemStack) {
		return this;
	}

	@Override
	public Item getEmptyItem(ItemStack itemStack) {
		return this;
	}

	@Override
	public int getMaxCharge(ItemStack itemStack) {
		return this.maxCharge;
	}

	@Override
	public int getTier(ItemStack itemStack) {
		return this.tier;
	}

	@Override
	public int getTransferLimit(ItemStack itemStack) {
		return this.transferLimit;
	}
		
}

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

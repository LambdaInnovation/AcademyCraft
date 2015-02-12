package cn.academy.energy.item;

import java.util.List;

import cn.academy.core.AcademyCraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItemManager;
import ic2.api.item.ISpecialElectricItem;

/**
 * 能量水晶
 * @author Lyt99
 */
public class ItemEnergyCrystal extends Item implements ISpecialElectricItem {
			
	protected IIcon[] textures;
	protected int maxCharge = 500000, tier = 2, transferLimit = 128;

	public ItemEnergyCrystal(){
		setUnlocalizedName("ac_energycrystal");
		setCreativeTab(AcademyCraft.cct);
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
	public IIcon getIconIndex(ItemStack par1){
		return getIconFromDamage(getItemCharge(par1));
	}

	@Override
	public int getMaxDamage(){
		return this.maxCharge;
	}
	
	@Override
	public int getMaxDamage(ItemStack stack){
		return maxCharge;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs par2CreativeTabs, List par3List){
		ItemStack charged = new ItemStack(this, 1);
		ItemStack discharged = new ItemStack(this, 1);
		par3List.add(discharged);
		setItemCharge(charged,this.maxCharge);
		par3List.add(charged);
	}
	
	@Override
	public void addInformation(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4){
		super.addInformation(par1ItemStack, par2EntityPlayer, par3List, par4);
			par3List.add(StatCollector.translateToLocal("gui.remaining.energy")+ ": " + getItemCharge(par1ItemStack) + "/" + this.maxCharge + " " + StatCollector.translateToLocal("gui.energy.unit"));
	}
	
	@Override
	public boolean isDamaged(ItemStack stack){
		return getItemCharge(stack) < maxCharge;
	}
	
	@Override
	public int getDisplayDamage(ItemStack stack){
		return maxCharge - getItemCharge(stack);
	}
	
	@Override
	public int getDamage(ItemStack stack){
		return maxCharge - getItemCharge(stack);
	}
	
	@Override
	public void setDamage(ItemStack stack, int damage){
		setItemCharge(stack, this.maxCharge - damage);
	}

	
	protected int getItemCharge(ItemStack stack) {
		return loadCompound(stack).getInteger("charge");
	}
	
	private NBTTagCompound loadCompound(ItemStack stack) {
		if (stack.stackTagCompound == null)
			stack.stackTagCompound = new NBTTagCompound();
		return stack.stackTagCompound;
	}

	protected void setItemCharge(ItemStack stack, int charge){
		loadCompound(stack).setInteger(
				"charge",
				(charge > 0) ? (charge > this.maxCharge ? this.maxCharge
						: charge) : 0);
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

	@Override
	public IElectricItemManager getManager(ItemStack itemStack) {
		return ElectricItem.manager;
	}
		
}

/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.template;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import cn.academy.core.item.ACItem;
import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.api.item.ImagEnergyItem;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 *
 */
public class ItemEnergyBase extends ACItem implements ImagEnergyItem {
	
	protected static IFItemManager itemManager = IFItemManager.instance;
	
	public final String name;
	
	public final double maxEnergy;
	public final double bandwidth;
	
	IIcon iconEmpty, iconHalf, iconFull;
	public boolean useMultipleIcon = true;
	
	public ItemEnergyBase(String _name, double _maxEnergy, double _bandwidth) {
		super(_name);
		name = _name;
		maxEnergy = _maxEnergy;
		bandwidth = _bandwidth;
		
		setMaxStackSize(1);
		setMaxDamage(13);
	}
	
    @Override
	@SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister ir) {
    	if(useMultipleIcon) {
	    	iconEmpty = ir.registerIcon("academy:" + name + "_empty");
	    	iconHalf = ir.registerIcon("academy:" + name + "_half");
	    	iconFull = ir.registerIcon("academy:" + name + "_full");
    	} else {
    		super.registerIcons(ir);
    	}
    }

    @Override
	@SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
    	if(!useMultipleIcon)
    		return super.getIconFromDamage(damage);
    	
    	if(damage < 3) {
    		return iconFull;
    	}
    	if(damage > 10) {
    		return iconEmpty;
    	}
    	return iconHalf;
    }
    
	@Override
	public double getMaxEnergy() {
		return maxEnergy;
	}

	@Override
	public double getBandwidth() {
		return bandwidth;
	}
	
    @Override
	@SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs cct, List list) {
    	ItemStack is = new ItemStack(this);
    	list.add(is);
    	itemManager.charge(is, 0, true);
    	
    	is = new ItemStack(this);
    	itemManager.charge(is, Double.MAX_VALUE, true);
    	list.add(is);
    }
    
    @Override
	@SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean wtf) {
    	list.add(itemManager.getDescription(stack));
    }

}

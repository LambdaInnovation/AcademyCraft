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
package cn.academy.energy.api.item;

import net.minecraft.item.ItemStack;
import cn.academy.support.EnergyHelper.EnergyItemManager;
import cn.liutils.util.mc.StackUtils;

/**
 * @author WeathFolD
 */
public final class IFItemManager implements EnergyItemManager {
	
	public static IFItemManager instance = new IFItemManager();
	
	private IFItemManager() {}
    
    public double getEnergy(ItemStack stack) {
    	ImagEnergyItem item = (ImagEnergyItem) stack.getItem();
		return StackUtils.loadTag(stack).getDouble("energy");
    }
    
    public double getMaxEnergy(ItemStack stack) {
    	ImagEnergyItem item = (ImagEnergyItem) stack.getItem();
		return item.getMaxEnergy();
    }
    
    public void setEnergy(ItemStack stack, double amt) {
    	ImagEnergyItem item = (ImagEnergyItem) stack.getItem();
		amt = Math.min(item.getMaxEnergy(), amt);
		StackUtils.loadTag(stack).setDouble("energy", amt);
		
		int approxDamage = (int) Math.round((1 - amt / getMaxEnergy(stack)) * stack.getMaxDamage());
		stack.setItemDamage(approxDamage);
    }
    
    public double charge(ItemStack stack, double amt) {
    	return charge(stack, amt, false);
    }
    
    /**
     * @param stack
     * @param amt Energy trying to charge into stack, can be neigative
     * @param ignoreLatency
     * @return How much energy not transfered into stack
     */
    public double charge(ItemStack stack, double amt, boolean ignoreLatency) {
    	ImagEnergyItem item = (ImagEnergyItem) stack.getItem();
		double lim = ignoreLatency ? Double.MAX_VALUE : item.getLatency();
		double cur = getEnergy(stack);
		double spare = 0.0;
		if(amt + cur > item.getMaxEnergy()) {
			spare = cur + amt - item.getMaxEnergy();
			amt = item.getMaxEnergy() - cur;
		}
		if(amt + cur < 0) {
			spare = amt + cur;
			amt = -cur;
		}
		
		double namt = Math.signum(amt) * Math.min(Math.abs(amt), lim);
		spare += amt - namt;
		
		setEnergy(stack, cur + namt);
		return spare;
    }
    
    public String getDescription(ItemStack stack) {
    	return String.format("%.0f/%.0f IF", getEnergy(stack), getMaxEnergy(stack));
    }

	@Override
	public boolean isSupported(ItemStack stack) {
		return stack.getItem() instanceof ImagEnergyItem;
	}
    
}

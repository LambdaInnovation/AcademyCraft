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
package cn.academy.support;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cn.academy.energy.api.item.IFItemManager;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegInit;

/**
 * A generic-typed energy helper. Preserved for future use.
 * @author WeAthFolD
 */
@Registrant
@RegInit
public class EnergyHelper {

	static List<EnergyItemManager> supported = new ArrayList();
	
	public static void init() {
		supported.add(IFItemManager.instance);
	}
	
	public static boolean isSupported(ItemStack stack) {
		for(EnergyItemManager m : supported) {
			if(m.isSupported(stack))
				return true;
		}
		return false;
	}
	
	public static double getEnergy(ItemStack stack) {
		for(EnergyItemManager m : supported) {
			if(m.isSupported(stack))
				return m.getEnergy(stack);
		}
		return 0.0;
	}
	
	public static void setEnergy(ItemStack stack, double energy) {
		for(EnergyItemManager m : supported) {
			if(m.isSupported(stack)) {
				m.setEnergy(stack, energy);
				return;
			}
		}
	}
	
	public static double charge(ItemStack stack, double amt, boolean ignoreLatency) {
		for(EnergyItemManager m : supported) {
			if(m.isSupported(stack)) {
				return m.charge(stack, amt, ignoreLatency);
			}
		}
		return amt;
	}
	
	public static ItemStack createEmptyItem(Item item) {
		ItemStack ret = new ItemStack(item);
		charge(ret, 0, true);
		return ret;
	}
	
	public static ItemStack createFullItem(Item item) {
		ItemStack ret = new ItemStack(item);
		charge(ret, Integer.MAX_VALUE, true);
		return ret;
	}
	
	public interface EnergyItemManager {
		
		boolean isSupported(ItemStack stack);
		
		double getEnergy(ItemStack stack);
		
		void setEnergy(ItemStack stack, double energy);
		
		double charge(ItemStack stack, double amt, boolean ignoreLatency);
		
	}
	
}

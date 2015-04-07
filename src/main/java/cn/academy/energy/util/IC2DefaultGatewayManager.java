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
package cn.academy.energy.util;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IElectricItemManager;
import ic2.api.item.ISpecialElectricItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * IC2 default energy manager implementation. All rights reserved by
 * IndustrialCraft2's original developers.
 */
public class IC2DefaultGatewayManager implements IElectricItemManager {
	  public double charge(ItemStack itemStack, double amount, int tier, boolean ignoreTransferLimit, boolean simulate)
	  {
	    IElectricItemManager manager = getManager(itemStack);
	    if (manager == null) return 0.0D;

	    return manager.charge(itemStack, amount, tier, ignoreTransferLimit, simulate);
	  }

	  public double discharge(ItemStack itemStack, double amount, int tier, boolean ignoreTransferLimit, boolean externally, boolean simulate)
	  {
	    IElectricItemManager manager = getManager(itemStack);
	    if (manager == null) return 0.0D;

	    return manager.discharge(itemStack, amount, tier, ignoreTransferLimit, externally, simulate);
	  }

	  public double getCharge(ItemStack itemStack)
	  {
	    IElectricItemManager manager = getManager(itemStack);
	    if (manager == null) return 0.0D;

	    return manager.getCharge(itemStack);
	  }

	  public boolean canUse(ItemStack itemStack, double amount)
	  {
	    IElectricItemManager manager = getManager(itemStack);
	    if (manager == null) return false;

	    return manager.canUse(itemStack, amount);
	  }

	  public boolean use(ItemStack itemStack, double amount, EntityLivingBase entity)
	  {
	    IElectricItemManager manager = getManager(itemStack);
	    if (manager == null) return false;

	    return manager.use(itemStack, amount, entity);
	  }

	  public void chargeFromArmor(ItemStack itemStack, EntityLivingBase entity)
	  {
	    if (entity == null) return;

	    IElectricItemManager manager = getManager(itemStack);
	    if (manager == null) return;

	    manager.chargeFromArmor(itemStack, entity);
	  }

	  public String getToolTip(ItemStack itemStack)
	  {
	    IElectricItemManager manager = getManager(itemStack);
	    if (manager == null) return null;

	    return manager.getToolTip(itemStack);
	  }

	  private IElectricItemManager getManager(ItemStack stack) {
	    Item item = stack.getItem();
	    if (item == null) return null;

	    if ((item instanceof ISpecialElectricItem))
	      return ((ISpecialElectricItem)item).getManager(stack);
	    if ((item instanceof IElectricItem)) {
	      return ElectricItem.rawManager;
	    }
	    return ElectricItem.getBackupManager(stack);
	  }
}
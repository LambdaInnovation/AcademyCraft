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
import ic2.api.item.ICustomElectricItem;
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

	@Override
	public int charge(ItemStack itemStack, int amount, int tier,
			boolean ignoreTransferLimit, boolean simulate) {
		Item item = itemStack.getItem();

		if (!(item instanceof IElectricItem))
			return 0;

		if ((item instanceof ICustomElectricItem))
			return ((ICustomElectricItem) item).charge(itemStack, amount, tier,
					ignoreTransferLimit, simulate);
		if ((item instanceof ISpecialElectricItem)) {
			return ((ISpecialElectricItem) item).getManager(itemStack).charge(
					itemStack, amount, tier, ignoreTransferLimit, simulate);
		}
		return ElectricItem.rawManager.charge(itemStack, amount, tier,
				ignoreTransferLimit, simulate);
	}

	@Override
	public int discharge(ItemStack itemStack, int amount, int tier,
			boolean ignoreTransferLimit, boolean simulate) {
		Item item = itemStack.getItem();

		if (!(item instanceof IElectricItem))
			return 0;

		if ((item instanceof ICustomElectricItem))
			return ((ICustomElectricItem) item).discharge(itemStack, amount,
					tier, ignoreTransferLimit, simulate);
		if ((item instanceof ISpecialElectricItem)) {
			return ((ISpecialElectricItem) item).getManager(itemStack)
					.discharge(itemStack, amount, tier, ignoreTransferLimit,
							simulate);
		}
		return ElectricItem.rawManager.discharge(itemStack, amount, tier,
				ignoreTransferLimit, simulate);
	}

	@Override
	public int getCharge(ItemStack itemStack) {
		Item item = itemStack.getItem();

		if (!(item instanceof IElectricItem))
			return 0;

		if ((item instanceof ISpecialElectricItem)) {
			return ((ISpecialElectricItem) item).getManager(itemStack)
					.getCharge(itemStack);
		}
		return ElectricItem.rawManager.getCharge(itemStack);
	}

	@Override
	public boolean canUse(ItemStack itemStack, int amount) {
		Item item = itemStack.getItem();

		if (!(item instanceof IElectricItem))
			return false;

		if ((item instanceof ICustomElectricItem))
			return ((ICustomElectricItem) item).canUse(itemStack, amount);
		if ((item instanceof ISpecialElectricItem)) {
			return ((ISpecialElectricItem) item).getManager(itemStack).canUse(
					itemStack, amount);
		}
		return ElectricItem.rawManager.canUse(itemStack, amount);
	}

	@Override
	public boolean use(ItemStack itemStack, int amount, EntityLivingBase entity) {
		Item item = itemStack.getItem();

		if (!(item instanceof IElectricItem))
			return false;

		if ((item instanceof ISpecialElectricItem)) {
			return ((ISpecialElectricItem) item).getManager(itemStack).use(
					itemStack, amount, entity);
		}
		return ElectricItem.rawManager.use(itemStack, amount, entity);
	}

	@Override
	public void chargeFromArmor(ItemStack itemStack, EntityLivingBase entity) {
		Item item = itemStack.getItem();

		if ((entity == null) || (!(item instanceof IElectricItem)))
			return;

		if ((item instanceof ISpecialElectricItem))
			((ISpecialElectricItem) item).getManager(itemStack)
					.chargeFromArmor(itemStack, entity);
		else
			ElectricItem.rawManager.chargeFromArmor(itemStack, entity);
	}

	@Override
	public String getToolTip(ItemStack itemStack) {
		Item item = itemStack.getItem();

		if (!(item instanceof IElectricItem))
			return null;

		if ((item instanceof ICustomElectricItem)) {
			if (((ICustomElectricItem) item).canShowChargeToolTip(itemStack)) {
				return ElectricItem.rawManager.getToolTip(itemStack);
			}
			return null;
		}
		if ((item instanceof ISpecialElectricItem)) {
			return ((ISpecialElectricItem) item).getManager(itemStack)
					.getToolTip(itemStack);
		}
		return ElectricItem.rawManager.getToolTip(itemStack);
	}
}
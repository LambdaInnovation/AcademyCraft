package cn.academy.energy.util;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IElectricItemManager;
import cn.liutils.util.GenericUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * All credits of this class' code goes to IC2 team.
 */
public class IC2DefaultEIManager implements IElectricItemManager {
	public int charge(ItemStack stack, int charge, int tier,
			boolean ignoreTransferLimit, boolean simulate) {
		IElectricItem item = (IElectricItem) stack.getItem();

		assert (item.getMaxCharge(stack) > 0);

		if ((charge < 0) || (stack.stackSize > 1)
				|| (item.getTier(stack) > tier))
			return 0;

		if ((!ignoreTransferLimit) && (charge > item.getTransferLimit(stack)))
			charge = item.getTransferLimit(stack);

		NBTTagCompound tNBT = GenericUtils.loadCompound(stack);
		int newCharge = tNBT.getInteger("charge");

		if (charge > item.getMaxCharge(stack) - newCharge)
			charge = item.getMaxCharge(stack) - newCharge;

		if (!simulate) {
			newCharge += charge;

			if (newCharge > 0) {
				tNBT.setInteger("charge", newCharge);
			} else {
				tNBT.removeTag("charge");
				if (tNBT.hasNoTags())
					stack.setTagCompound(null);
			}

			stack.func_150996_a(newCharge > 0 ? item.getChargedItem(stack)
					: item.getEmptyItem(stack));

			if ((stack.getItem() instanceof IElectricItem)) {
				item = (IElectricItem) stack.getItem();

				if (stack.getMaxDamage() > 2)
					stack.setItemDamage((int) (1L + (item.getMaxCharge(stack) - newCharge)
							* (stack.getMaxDamage() - 2L)
							/ item.getMaxCharge(stack)));
				else
					stack.setItemDamage(0);
			} else {
				stack.setItemDamage(0);
			}
		}

		return charge;
	}

	public int discharge(ItemStack stack, int charge, int tier,
			boolean ignoreTransferLimit, boolean simulate) {
		IElectricItem item = (IElectricItem) stack.getItem();

		assert (item.getMaxCharge(stack) > 0);

		if ((charge < 0) || (stack.stackSize > 1)
				|| (item.getTier(stack) > tier))
			return 0;

		if ((!ignoreTransferLimit) && (charge > item.getTransferLimit(stack)))
			charge = item.getTransferLimit(stack);

		NBTTagCompound tNBT = GenericUtils.loadCompound(stack);
		int newCharge = tNBT.getInteger("charge");

		if (charge > newCharge)
			charge = newCharge;

		if (!simulate) {
			newCharge -= charge;

			if (newCharge > 0) {
				tNBT.setInteger("charge", newCharge);
			} else {
				tNBT.removeTag("charge");
				if (tNBT.hasNoTags())
					stack.setTagCompound(null);
			}

			stack.func_150996_a(newCharge > 0 ? item.getChargedItem(stack)
					: item.getEmptyItem(stack));

			if ((stack.getItem() instanceof IElectricItem)) {
				item = (IElectricItem) stack.getItem();

				if (stack.getMaxDamage() > 2)
					stack.setItemDamage((int) (1L + (item.getMaxCharge(stack) - newCharge)
							* (stack.getMaxDamage() - 2L)
							/ item.getMaxCharge(stack)));
				else
					stack.setItemDamage(0);
			} else {
				stack.setItemDamage(0);
			}
		}

		return charge;
	}

	public int getCharge(ItemStack itemStack) {
		return ElectricItem.manager.discharge(itemStack, 2147483647,
				2147483647, true, true);
	}

	public boolean canUse(ItemStack itemStack, int amount) {
		return ElectricItem.manager.getCharge(itemStack) >= amount;
	}

	public boolean use(ItemStack itemStack, int amount, EntityLivingBase entity) {
		ElectricItem.manager.chargeFromArmor(itemStack, entity);

		int transfer = ElectricItem.manager.discharge(itemStack, amount,
				2147483647, true, true);

		if (transfer == amount) {
			ElectricItem.manager.discharge(itemStack, amount, 2147483647, true,
					false);
			ElectricItem.manager.chargeFromArmor(itemStack, entity);

			return true;
		}
		return false;
	}

	public void chargeFromArmor(ItemStack itemStack, EntityLivingBase entity) {
		boolean inventoryChanged = false;

		for (int i = 0; i < 4; i++) {
			ItemStack armorItemStack = entity.getEquipmentInSlot(i + 1);

			if ((armorItemStack != null)
					&& ((armorItemStack.getItem() instanceof IElectricItem))) {
				IElectricItem armorItem = (IElectricItem) armorItemStack
						.getItem();

				if ((armorItem.canProvideEnergy(armorItemStack))
						&& (armorItem.getTier(armorItemStack) >= ((IElectricItem) itemStack
								.getItem()).getTier(itemStack))) {
					int transfer = ElectricItem.manager.charge(itemStack,
							2147483647, 2147483647, true, true);

					transfer = ElectricItem.manager.discharge(armorItemStack,
							transfer, 2147483647, true, false);

					if (transfer > 0) {
						ElectricItem.manager.charge(itemStack, transfer,
								2147483647, true, false);

						inventoryChanged = true;
					}
				}
			}
		}
	}

	public String getToolTip(ItemStack itemStack) {
		IElectricItem item = (IElectricItem) itemStack.getItem();

		return ElectricItem.manager.getCharge(itemStack) + "/"
				+ item.getMaxCharge(itemStack) + " EU";
	}
}
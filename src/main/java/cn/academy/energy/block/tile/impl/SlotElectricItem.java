/**
 * 
 */
package cn.academy.energy.block.tile.impl;

import cn.academy.energy.util.EnergyUtils;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * @author WeathFolD
 *
 */
public class SlotElectricItem extends Slot {

	public SlotElectricItem(IInventory inv, int slot, int x, int y) {
		super(inv, slot, x, y);
	}
	
    @Override
	public boolean isItemValid(ItemStack stack) {
        return EnergyUtils.isElecItem(stack);
    }

}

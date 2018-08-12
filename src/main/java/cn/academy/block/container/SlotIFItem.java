package cn.academy.block.container;

import cn.academy.energy.api.IFItemManager;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * @author WeAthFolD
 *
 */
public class SlotIFItem extends Slot {

    public SlotIFItem(IInventory inv, int slot, int x, int y) {
        super(inv, slot, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return (stack != null && IFItemManager.instance.isSupported(stack));
    }

}
package cn.academy.block.container;

import cn.academy.crafting.MetalFormerRecipes;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * @author KSkun
 */
public class SlotMFItem extends Slot {

    private int slot;

    public SlotMFItem(IInventory inv, int _slot, int x, int y) {
        super(inv, _slot, x, y);
        slot = _slot;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        if(slot == 0) {
            for (MetalFormerRecipes.RecipeObject obj : MetalFormerRecipes.INSTANCE.getAllRecipes()) {
                if (obj.input.getItem() == stack.getItem()) return true;
            }
        }
        return false;
    }

}
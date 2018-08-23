package cn.academy.block.container;

import cn.academy.ACItems;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * @author WeAthFolD
 */
public class SlotFan extends Slot {

    public SlotFan(IInventory p_i1824_1_, int p_i1824_2_, int p_i1824_3_,
            int p_i1824_4_) {
        super(p_i1824_1_, p_i1824_2_, p_i1824_3_, p_i1824_4_);
    }
    
    @Override
    public boolean isItemValid(ItemStack stack) {
        return (stack != null && stack.getItem() == ACItems.windgen_fan);
    }

}
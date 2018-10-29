package cn.academy.item;

import net.minecraft.util.NonNullList;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * @author WeAthFolD
 */
public class ItemMatrixCore extends Item {
    
    int LEVELS = 3;

    public ItemMatrixCore() {
        this.setHasSubtypes(true);
    }
    
    @Override
    public String getTranslationKey(ItemStack stack) {
        return super.getTranslationKey(stack) + "_" + stack.getItemDamage();
    }
    
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (isInCreativeTab(tab)) {
            for(int i = 0; i < LEVELS; ++i)
                items.add(new ItemStack(this, 1, i));
        }
    }

}
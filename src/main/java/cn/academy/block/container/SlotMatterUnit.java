package cn.academy.block.container;

import cn.academy.ACItems;
import cn.academy.worldgen.WorldGenInit;
import cn.academy.item.ItemMatterUnit.MatterMaterial;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * @author WeAthFolD
 */
public class SlotMatterUnit extends Slot {
    
    final MatterMaterial material;

    public SlotMatterUnit(IInventory inv, int slot, int x, int y) {
        this(inv, null, slot, x, y);
    }
    
    public SlotMatterUnit(IInventory inv, MatterMaterial mat, int slot, int x, int y) {
        super(inv, slot, x, y);
        material = mat;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return (stack != null && 
                stack.getItem() == ACItems.matter_unit &&
                (material == null || ACItems.matter_unit.getMaterial(stack) == material));
    }

}
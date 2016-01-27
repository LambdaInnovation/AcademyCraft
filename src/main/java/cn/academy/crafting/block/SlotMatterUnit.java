/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.crafting.block;

import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.item.ItemMatterUnit.MatterMaterial;
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
                stack.getItem() == ModuleCrafting.matterUnit && 
                (material == null || ModuleCrafting.matterUnit.getMaterial(stack) == material));
    }

}

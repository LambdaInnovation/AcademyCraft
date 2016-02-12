/**
 * Copyright (c) Lambda Innovation, 2013-2016
 * This file is part of the AcademyCraft mod.
 * https://github.com/LambdaInnovation/AcademyCraft
 * Licensed under GPLv3, see project root for more information.
 */
package cn.academy.crafting.block;

import cn.academy.crafting.api.ImagFusorRecipes;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * @author KSkun
 */
public class SlotCrystal extends Slot {

    private int slot;

    public SlotCrystal(IInventory inv, int _slot, int x, int y) {
        super(inv, _slot, x, y);
        slot = _slot;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        if(slot == 0) {
            for (ImagFusorRecipes.IFRecipe obj : ImagFusorRecipes.INSTANCE.getAllRecipe()) {
                if (obj.consumeType.getItem() == stack.getItem()) return true;
            }
        }
        return false;
    }

}

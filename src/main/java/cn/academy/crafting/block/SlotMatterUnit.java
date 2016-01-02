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
package cn.academy.crafting.block;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.item.ItemMatterUnit.MatterMaterial;

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

/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.crafting.block;

import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.item.ItemMatterUnit;
import cn.academy.energy.block.SlotIFItem;
import cn.academy.support.EnergyItemHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import static cn.academy.crafting.block.TileMetalFormer.SLOT_BATTERY;
import static cn.academy.crafting.block.TileMetalFormer.SLOT_IN;

/**
 * @author WeAthFolD
 */
public class ContainerMetalFormer extends Container {
    
    public final TileMetalFormer tile;
    public final EntityPlayer player;
    
    public ContainerMetalFormer(TileMetalFormer _tile, EntityPlayer _player) {
        tile = _tile;
        player = _player;
        
        initInventory();
    }
    
    private void initInventory() {
        InventoryPlayer inv = player.inventory;
        
        this.addSlotToContainer(new SlotMFItem(tile, 0, 15, 40));
        this.addSlotToContainer(new SlotMFItem(tile, 1, 145, 40));
        this.addSlotToContainer(new SlotIFItem(tile, 2, 44, 71));
        
        int STEP = 18;
        
        for(int i = 0; i < 9; ++i) {
            addSlotToContainer(new Slot(inv, i, 8 + i * STEP, 153));
        }
        
        for(int i = 1; i < 4; ++i) {
            for(int j = 0; j < 9; ++j) {
                int slot = (4 - i) * 9 + j;
                addSlotToContainer(new Slot(inv, slot, 8 + j * STEP, 149 - i * STEP));
            }
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int id) {
        ItemStack stack = null;
        Slot slot = (Slot)this.inventorySlots.get(id);

        if (slot != null && slot.getHasStack()) {
            ItemStack stack1 = slot.getStack();
            stack = stack1.copy();

            ItemMatterUnit unit = ModuleCrafting.matterUnit;
            
            if (id < 3) { //tileInv->playerInv
                if (!this.mergeItemStack(stack1, 4, this.inventorySlots.size(), true))
                    return null;
            } else {
                if(EnergyItemHelper.isSupported(stack)) {
                    if (!this.mergeItemStack(stack1, SLOT_BATTERY, SLOT_BATTERY + 1, false))  
                        return null;
                } else {
                    if (!this.mergeItemStack(stack1, SLOT_IN, SLOT_IN + 1, false))  
                        return null;
                }
            }

            if (stack1.stackSize == 0) {
                slot.putStack((ItemStack)null);
            } else {
                slot.onSlotChanged();
            }
        }

        return stack;
    }
    
    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

}

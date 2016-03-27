/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.block.wind;

import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.block.SlotIFItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * @author WeAthFolD
 */
public class ContainerWindGenBase extends Container {
    
    public final EntityPlayer player;
    public final TileWindGenBase tile;
    
    public ContainerWindGenBase(EntityPlayer _player, TileWindGenBase _tile) {
        player = _player;
        tile = _tile;
        
        initInventory();
    }
    
    private void initInventory() {
        this.addSlotToContainer(new SlotIFItem(tile, 0, 26, 70));
        
        InventoryPlayer inv = player.inventory;
        int STEP = 18;
        
        for(int i = 0; i < 9; ++i) {
            addSlotToContainer(new Slot(inv, i, -10 + i * STEP, 153));
        }
        
        for(int i = 1; i < 4; ++i) {
            for(int j = 0; j < 9; ++j) {
                int slot = (4 - i) * 9 + j;
                addSlotToContainer(new Slot(inv, slot, -10 + j * STEP, 149 - i * STEP));
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

            if (id < 1) { //tileInv->playerInv
                if (!this.mergeItemStack(stack1, 1, this.inventorySlots.size(), true)) {
                    return null;
                }
            } else {
                if(!IFItemManager.instance.isSupported(stack1) || 
                        !this.mergeItemStack(stack1, 0, 1, false)) { //playerInv->tileInv
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

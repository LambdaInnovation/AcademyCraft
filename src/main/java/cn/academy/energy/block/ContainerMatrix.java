/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.block;

import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * @author WeAthFolD
 *
 */
public class ContainerMatrix extends Container {
    
    public final TileMatrix tile;
    public final EntityPlayer player;
    
    public ContainerMatrix(TileMatrix _tile, EntityPlayer _player) {
        tile = _tile;
        player = _player;
        
        initInventory(player.inventory);
    }
    
    private void initInventory(InventoryPlayer inv) {
        this.addSlotToContainer(new SlotPlate(tile, 0, 27, 65));
        this.addSlotToContainer(new SlotPlate(tile, 1, 76, 65));
        this.addSlotToContainer(new SlotPlate(tile, 2, 50, 6));
        
        this.addSlotToContainer(new SlotCore(tile, 3, 50, 33));
        
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
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
    
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int id) {
        ItemStack stack = null;
        Slot slot = (Slot)this.inventorySlots.get(id);

        if (slot != null && slot.getHasStack()) {
            ItemStack stack1 = slot.getStack();
            stack = stack1.copy();

            if (id < 4) { //tileInv->playerInv
                if (!this.mergeItemStack(stack1, 4, this.inventorySlots.size(), true))
                    return null;
            } else {
                if(stack.getItem() == ModuleCrafting.constPlate) {
                    for(int s = 0; s < 3; ++s) {
                        if(tile.getStackInSlot(s) == null) {
                            stack1.stackSize--;
                            tile.setInventorySlotContents(s, new ItemStack(ModuleCrafting.constPlate));
                            break;
                        }
                    }
                    stack = null;
                } else if(stack.getItem() == ModuleEnergy.matrixCore) {
                    if(tile.getStackInSlot(3) == null) {
                        stack1.stackSize--;
                        tile.setInventorySlotContents(3, 
                            new ItemStack(ModuleEnergy.matrixCore, 1, stack1.getItemDamage()));
                    }
                    stack = null;
                } else 
                    return null;
            }

            if (stack1.stackSize == 0) {
                slot.putStack((ItemStack)null);
            } else {
                slot.onSlotChanged();
            }
        }

        return stack;
    }
    
    public static class SlotCore extends Slot {

        public SlotCore(IInventory inv, int slot, int x, int y) {
            super(inv, slot, x, y);
        }
        
        @Override
        public boolean isItemValid(ItemStack stack) {
            return stack != null && stack.getItem() == ModuleEnergy.matrixCore;
        }
        
    }
    
    public static class SlotPlate extends Slot {

        public SlotPlate(IInventory inv, int slot, int x, int y) {
            super(inv, slot, x, y);
        }
        
        @Override
        public boolean isItemValid(ItemStack stack) {
            return stack != null && stack.getItem() == ModuleCrafting.constPlate;
        }
        
    }

}

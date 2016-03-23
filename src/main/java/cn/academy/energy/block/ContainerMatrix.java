/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.block;

import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;
import cn.lambdalib.template.container.CleanContainer;
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
public class ContainerMatrix extends CleanContainer {
    
    public final TileMatrix tile;
    public final EntityPlayer player;
    
    public ContainerMatrix(TileMatrix _tile, EntityPlayer _player) {
        tile = _tile;
        player = _player;
        
        initInventory(player.inventory);
    }
    
    private void initInventory(InventoryPlayer inv) {
        this.addSlotToContainer(new SlotPlate(tile, 0, 62, 1));
        this.addSlotToContainer(new SlotPlate(tile, 1, 37, 50));
        this.addSlotToContainer(new SlotPlate(tile, 2, 88, 50));
        
        this.addSlotToContainer(new SlotCore(tile, 3, 62, 26));
        
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

        SlotGroup invGroup = gRange(4, 4 + 36);

        addTransferRule(invGroup, stack -> stack.getItem() == ModuleCrafting.constPlate, gSlots(0, 1, 2));
        addTransferRule(invGroup, stack -> stack.getItem() == ModuleEnergy.matrixCore, gSlots(3));
        addTransferRule(gRange(0, 4), invGroup);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
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

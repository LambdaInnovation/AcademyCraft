/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.crafting.block;

import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.api.ImagFusorRecipes;
import cn.academy.crafting.item.ItemMatterUnit;
import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.block.SlotIFItem;
import cn.lambdalib.template.container.CleanContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import static cn.academy.crafting.block.TileImagFusor.*;

/**
 * @author WeAthFolD
 *
 */
public class ContainerImagFusor extends CleanContainer {

    public final TileImagFusor tile;
    public final EntityPlayer player;
    
    public ContainerImagFusor(TileImagFusor _tile, EntityPlayer _player) {
        tile = _tile;
        player = _player;
        
        initInventory(player.inventory);
    }
    
    private void initInventory(InventoryPlayer inv) {
        this.addSlotToContainer(new SlotCrystal(tile, SLOT_INPUT, -3, 39));
        this.addSlotToContainer(new SlotCrystal(tile, SLOT_OUTPUT, 127, 39));
        this.addSlotToContainer(new SlotMatterUnit(tile, ModuleCrafting.imagPhase.mat, SLOT_IMAG_INPUT, -3, 0));
        this.addSlotToContainer(new SlotMatterUnit(tile, ModuleCrafting.imagPhase.mat, SLOT_IMAG_OUTPUT, 127, 0));
        this.addSlotToContainer(new SlotIFItem(tile, SLOT_ENERGY_INPUT, 26, 70));
        
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

        ItemMatterUnit unit = ModuleCrafting.matterUnit;
        SlotGroup inventoryGroup = gRange(4, inventorySlots.size());

        this.addTransferRule(inventoryGroup,
                stack -> unit.getMaterial(stack) == ModuleCrafting.imagPhase.mat,
                gSlots(2));

        this.addTransferRule(inventoryGroup,
                stack -> IFItemManager.instance.isSupported(stack),
                gSlots(3));

        this.addTransferRule(inventoryGroup, gSlots(0));

        this.addTransferRule(gRange(0, 4), inventoryGroup);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return player.getDistance(tile.xCoord, tile.yCoord, tile.zCoord) < 64;
    }

    /**
     * @author KSkun
     */
    private static class SlotCrystal extends Slot {

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
}

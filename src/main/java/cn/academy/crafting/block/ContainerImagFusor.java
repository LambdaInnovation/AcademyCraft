/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.crafting.block;

import cn.academy.core.container.TechUIContainer;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.api.ImagFusorRecipes;
import cn.academy.crafting.item.ItemMatterUnit;
import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.block.SlotIFItem;
import cn.lambdalib2.template.container.CleanContainer;
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
public class ContainerImagFusor extends TechUIContainer<TileImagFusor> {
    
    public ContainerImagFusor(TileImagFusor _tile, EntityPlayer _player) {
        super(_player, _tile);
        
        initInventory();
    }
    
    private void initInventory() {
        this.addSlotToContainer(new SlotCrystal(tile, SLOT_INPUT, 13, 49));
        this.addSlotToContainer(new SlotCrystal(tile, SLOT_OUTPUT, 143, 49));
        this.addSlotToContainer(new SlotMatterUnit(tile, ModuleCrafting.imagPhase.mat, SLOT_IMAG_INPUT, 13, 10));
        this.addSlotToContainer(new SlotMatterUnit(tile, ModuleCrafting.imagPhase.mat, SLOT_IMAG_OUTPUT, 143, 10));
        this.addSlotToContainer(new SlotIFItem(tile, SLOT_ENERGY_INPUT, 42, 80));
        
        mapPlayerInventory();

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

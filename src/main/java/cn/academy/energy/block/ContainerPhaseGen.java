/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.block;

import cn.academy.core.container.TechUIContainer;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.block.SlotMatterUnit;
import cn.academy.energy.api.IFItemManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * @author WeAthFolD
 */
public class ContainerPhaseGen extends TechUIContainer<TilePhaseGen> {
    
    public static final int SLOT_LIQUID_IN = 0, SLOT_LIQUID_OUT = 1, SLOT_OUTPUT = 2;

    public ContainerPhaseGen(EntityPlayer _player, TilePhaseGen _tile) {
        super(_player, _tile);
        
        initInventory();
    }
    
    private void initInventory() {
        this.addSlotToContainer(new SlotMatterUnit(tile, ModuleCrafting.imagPhase.mat, SLOT_LIQUID_IN, 45, 12));
        this.addSlotToContainer(new SlotMatterUnit(tile, ModuleCrafting.imagPhase.mat, SLOT_LIQUID_OUT, 112, 51));
        this.addSlotToContainer(new SlotIFItem(tile, SLOT_OUTPUT, 42, 80));
        
        mapPlayerInventory();

        SlotGroup gMachine = gRange(0, 3);
        SlotGroup gInv = gRange(4, 4+36);

        addTransferRule(gMachine, gInv);
        addTransferRule(gInv, IFItemManager.instance::isSupported, gSlots(SLOT_OUTPUT));
        addTransferRule(gInv, stack -> ModuleCrafting.matterUnit.getMaterial(stack) == ModuleCrafting.imagPhase.mat, gSlots(SLOT_LIQUID_IN));
    }

}

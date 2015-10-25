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
package cn.academy.energy.block;

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
public class ContainerPhaseGen extends Container {
	
	public static final int SLOT_LIQUID_IN = 0, SLOT_LIQUID_OUT = 1, SLOT_OUTPUT = 2;
	
	public final TilePhaseGen tile;
	public final EntityPlayer player;

	public ContainerPhaseGen(EntityPlayer _player, TilePhaseGen _tile) {
		player = _player;
		tile = _tile;
		
		initInventory();
	}
	
	private void initInventory() {
		this.addSlotToContainer(new SlotMatterUnit(tile, ModuleCrafting.imagPhase.mat, SLOT_LIQUID_IN, 15, 4));
		this.addSlotToContainer(new SlotMatterUnit(tile, ModuleCrafting.imagPhase.mat, SLOT_LIQUID_OUT, 81, 44));
        this.addSlotToContainer(new SlotIFItem(tile, SLOT_OUTPUT, 78, 71));
        
        InventoryPlayer inv = player.inventory;
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

            if (id < 3) { //tileInv->playerInv
                if (!this.mergeItemStack(stack1, 2, this.inventorySlots.size(), true))
                    return null;
            } else { 
            	if(IFItemManager.instance.isSupported(stack1)) {
            		if(!this.mergeItemStack(stack1, TilePhaseGen.SLOT_OUTPUT, TilePhaseGen.SLOT_OUTPUT + 1, false))
            			return null;
            	} else if(ModuleCrafting.matterUnit.getMaterial(stack1) == ModuleCrafting.imagPhase.mat) {
            		if(!this.mergeItemStack(stack1, TilePhaseGen.SLOT_LIQUID_IN, TilePhaseGen.SLOT_LIQUID_IN + 1, false))
            			return null;
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
	
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return true;
	}

}

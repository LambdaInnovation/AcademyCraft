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

import static cn.academy.crafting.block.TileMetalFormer.*;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.item.ItemMatterUnit;
import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.block.SlotIFItem;
import cn.academy.support.EnergyItemHelper;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.annotations.GuiCallback;
import cn.liutils.cgui.gui.component.ProgressBar;
import cn.liutils.cgui.gui.event.FrameEvent;

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
		
		this.addSlotToContainer(new Slot(tile, 0, 15, 35));
    	this.addSlotToContainer(new Slot(tile, 1, 79, 35));
        this.addSlotToContainer(new SlotIFItem(tile, 2, 38, 71));
        
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

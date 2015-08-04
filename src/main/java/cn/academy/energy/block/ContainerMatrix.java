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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;

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
            	if(stack.getItem() == ModuleEnergy.constPlate) {
            		for(int s = 0; s < 3; ++s) {
            			if(tile.getStackInSlot(s) == null) {
            				stack1.stackSize--;
            				tile.setInventorySlotContents(s, new ItemStack(ModuleEnergy.constPlate));
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
	        return stack != null && stack.getItem() == ModuleEnergy.constPlate;
	    }
		
	}

}

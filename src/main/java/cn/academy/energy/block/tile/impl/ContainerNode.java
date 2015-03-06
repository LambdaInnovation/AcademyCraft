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
package cn.academy.energy.block.tile.impl;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import cn.academy.energy.util.EnergyUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
public class ContainerNode extends Container {
	
	public final TileNode node;
	
	boolean loaded = false;

	public ContainerNode(TileNode _node, EntityPlayer player) {
		node = _node;
		addSlotToContainer(new SlotElectricItem(node, 0, 83, 10));
		bindInv(player.inventory);
	}
	
	private void bindInv(InventoryPlayer inv) {
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 9; j++) {
				addSlotToContainer(new Slot(inv, j + i * 9 + 9,
						6 + j * 19, 96 + i * 19));
			}
		}
		for (int i = 0; i < 9; i++) {
			addSlotToContainer(new Slot(inv, i, 6 + i * 19, 157));
		}
	}
	
	@Override
    public void detectAndSendChanges() {
		super.detectAndSendChanges();
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public void updateProgressBar(int par1, int par2) {
    	loaded = true;
    }

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return node.isUseableByPlayer(player);
	}
	
    /**
     * Called when a player shift-clicks on a slot. You must override this or you will crash when someone does that.
     * WeAthFolD：↑MC傻逼代码
     */
    @Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slot) {
    	ItemStack stack = null;
		Slot slotObject = (Slot) inventorySlots.get(slot);
		// null checks and checks if the item can be stacked (maxStackSize > 1)
		if (slotObject != null && slotObject.getHasStack()) {
			ItemStack stackInSlot = slotObject.getStack();
			stack = stackInSlot.copy();

			// places it into the tileEntity is possible since its in the player
			// inventory
			if (slot == 0) {
				if (stackInSlot != null) {
					if (!this.mergeItemStack(stackInSlot, 1, 37, true)) {
						return null;
					}
				}
			}
			// merges the item into player inventory since its in the tileEntity
			else {
				if(!EnergyUtils.isElecItem(stackInSlot))
					return null;
				if (!this.mergeItemStack(stackInSlot, 0, 1, false))
					return null;
			}

			if (stackInSlot.stackSize == 0) {
				slotObject.putStack(null);
			} else {
				slotObject.onSlotChanged();
			}

			if (stackInSlot.stackSize == stack.stackSize) {
				return null;
			}
			slotObject.onPickupFromSlot(player, stackInSlot);
		}
		return stack;
    }

	public boolean isLoaded() {
		return loaded;
	}
	
}

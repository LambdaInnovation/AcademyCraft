/**
 * 
 */
package cn.academy.energy.block.tile.impl;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;

/**
 * @author WeathFolD
 *
 */
public class ContainerNode extends Container {
	
	public final TileNode node;
	
	boolean loaded = false;

	public ContainerNode(TileNode _node, EntityPlayer player) {
		node = _node;
		addSlotToContainer(new Slot(node, 0, 83, 10));
		bindInv(player.inventory);
	}
	
	private void bindInv(InventoryPlayer inv) {
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 9; j++) {
				addSlotToContainer(new Slot(inv, j + i * 9 + 9,
						(int) 6 + j * 19, 96 + i * 19));
			}
		}
		for (int i = 0; i < 9; i++) {
			addSlotToContainer(new Slot(inv, i, 6 + i * 19, 157));
		}
	}
	
	@Override
    public void detectAndSendChanges() {
		super.detectAndSendChanges();
		System.out.println("dasc");
		for(int i = 0; i < this.crafters.size(); ++i) {
			ICrafting ic = (ICrafting) this.crafters.get(i);
			ic.sendProgressBarUpdate(this, 0, (int) node.getEnergy() / 3);
		}
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public void updateProgressBar(int par1, int par2) {
    	loaded = true;
    	switch(par1) {
    	case 0:
    		node.setEnergy(par2 * 3);
    	}
    }

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return node.isUseableByPlayer(player);
	}

	public boolean isLoaded() {
		return loaded;
	}
	
}

/**
 * 
 */
package cn.academy.energy.block.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import cn.academy.energy.block.tile.impl.TileNode;

/**
 * @author WeathFolD
 *
 */
public class ContainerNode extends Container {
	
	final TileNode node;

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
	public boolean canInteractWith(EntityPlayer player) {
		return node.isUseableByPlayer(player);
	}

}

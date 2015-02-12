/**
 * 
 */
package cn.academy.misc.block.energy.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import cn.academy.misc.block.energy.tile.impl.TileNode;

/**
 * @author WeathFolD
 *
 */
public class ContainerNode extends Container {
	
	final TileNode node;

	public ContainerNode(TileNode _node, EntityPlayer player) {
		node = _node;
		addSlotToContainer(new Slot(node, 0, 73, 20));
		bindInv(player.inventory);
	}
	
	private void bindInv(InventoryPlayer inv) {
		
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return node.isUseableByPlayer(player);
	}

}

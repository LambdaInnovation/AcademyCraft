package cn.academy.energy.block;

import cn.academy.core.container.TechUIContainer;
import cn.academy.energy.api.IFItemManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * @author WeathFolD
 */
public class ContainerNode extends TechUIContainer<TileNode> {

    public ContainerNode(TileNode _node, EntityPlayer player) {
        super(player, _node);
        initInventory();
    }
    
    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
    }
    
    private void initInventory() {
        this.addSlotToContainer(new SlotIFItem(tile, 0, 42, 10));
        this.addSlotToContainer(new SlotIFItem(tile, 1, 42, 80));
        
        mapPlayerInventory();

        SlotGroup gBatteries = gSlots(0, 1);
        SlotGroup gInv = gRange(2, 2+36);

        addTransferRule(gBatteries, gInv);
        addTransferRule(gInv, IFItemManager.instance::isSupported, gBatteries);
    }

}
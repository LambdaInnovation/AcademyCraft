package cn.academy.block.container;

import cn.academy.energy.api.IFItemManager;
import cn.academy.block.tileentity.TileWindGenBase;
import net.minecraft.entity.player.EntityPlayer;

/**
 * @author WeAthFolD
 */
public class ContainerWindGenBase extends TechUIContainer<TileWindGenBase> {
    
    public ContainerWindGenBase(EntityPlayer _player, TileWindGenBase _tile) {
        super(_player, _tile);
        
        initInventory();
    }
    
    private void initInventory() {
        this.addSlotToContainer(new SlotIFItem(tile, 0, 42, 80));
        
        mapPlayerInventory();

        SlotGroup gInv = gRange(1, 1+36), gBattery = gSlots(0);

        addTransferRule(gBattery, gInv);
        addTransferRule(gInv, IFItemManager.instance::isSupported, gBattery);
    }

}
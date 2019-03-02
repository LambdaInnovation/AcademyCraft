package cn.academy.block.container;

import cn.academy.block.tileentity.TileMetalFormer;
import cn.academy.support.EnergyItemHelper;
import net.minecraft.entity.player.EntityPlayer;

import static cn.academy.block.tileentity.TileMetalFormer.*;

/**
 * @author WeAthFolD
 */
public class ContainerMetalFormer extends TechUIContainer<TileMetalFormer> {
    
    public ContainerMetalFormer(TileMetalFormer _tile, EntityPlayer _player) {
        super(_player, _tile);
        
        initInventory();
    }
    
    private void initInventory() {
        this.addSlotToContainer(new SlotMFItem(tile, 0, 13, 49));
        this.addSlotToContainer(new SlotMFItem(tile, 1, 143, 49));
        this.addSlotToContainer(new SlotIFItem(tile, 2, 42, 80));
        
        mapPlayerInventory();

        SlotGroup gInv = gRange(3, 3+36), gMachine = gSlots(0, 1, 2);

        addTransferRule(gMachine, gInv);
        addTransferRule(gInv, EnergyItemHelper::isSupported, gSlots(SLOT_BATTERY));
        addTransferRule(gInv, gSlots(SLOT_IN));
    }

}
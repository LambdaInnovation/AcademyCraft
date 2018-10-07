package cn.academy.block.container;

import cn.academy.ACBlocks;
import cn.academy.ACItems;
import cn.academy.item.ItemMatterUnit;
import cn.academy.worldgen.WorldGenInit;
import cn.academy.energy.api.IFItemManager;
import cn.academy.block.tileentity.TilePhaseGen;
import net.minecraft.entity.player.EntityPlayer;

/**
 * @author WeAthFolD
 */
public class ContainerPhaseGen extends TechUIContainer<TilePhaseGen> {
    
    public static final int SLOT_LIQUID_IN = 0, SLOT_LIQUID_OUT = 1, SLOT_OUTPUT = 2;

    public ContainerPhaseGen(EntityPlayer _player, TilePhaseGen _tile) {
        super(_player, _tile);
        
        initInventory();
    }
    
    private void initInventory() {
        this.addSlotToContainer(new SlotMatterUnit(tile, ItemMatterUnit.MAT_PHASE_LIQUID, SLOT_LIQUID_IN, 45, 12));
        this.addSlotToContainer(new SlotMatterUnit(tile, ItemMatterUnit.MAT_PHASE_LIQUID, SLOT_LIQUID_OUT, 112, 51));
        this.addSlotToContainer(new SlotIFItem(tile, SLOT_OUTPUT, 42, 80));
        
        mapPlayerInventory();

        SlotGroup gMachine = gRange(0, 3);
        SlotGroup gInv = gRange(4, 4+36);

        addTransferRule(gMachine, gInv);
        addTransferRule(gInv, IFItemManager.instance::isSupported, gSlots(SLOT_OUTPUT));
        addTransferRule(gInv, stack -> ACItems.matter_unit.getMaterial(stack) == ItemMatterUnit.MAT_PHASE_LIQUID, gSlots(SLOT_LIQUID_IN));
    }

}
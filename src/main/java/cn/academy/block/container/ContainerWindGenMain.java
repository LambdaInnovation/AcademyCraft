package cn.academy.block.container;

import cn.academy.ACItems;
import cn.academy.block.tileentity.TileWindGenMain;
import net.minecraft.entity.player.EntityPlayer;

/**
 * @author WeAthFolD
 */
public class ContainerWindGenMain extends TechUIContainer<TileWindGenMain> {
    
    public ContainerWindGenMain(EntityPlayer _player, TileWindGenMain _tile) {
        super(_player, _tile);

        this.addSlotToContainer(new SlotFan(tile, 0, 78, 9));

        mapPlayerInventory();

        SlotGroup gInv = gRange(1, 1+36), gFan = gSlots(0);

        addTransferRule(gFan, gInv);
        addTransferRule(gInv, stack -> stack.getItem() == ACItems.windgen_fan, gFan);
    }

}
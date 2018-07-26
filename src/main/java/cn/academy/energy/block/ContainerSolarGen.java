package cn.academy.energy.block;

import cn.academy.core.container.TechUIContainer;
import cn.academy.energy.api.IFItemManager;
import cn.lambdalib2.template.container.CleanContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

public class ContainerSolarGen extends TechUIContainer<TileSolarGen> {

    public ContainerSolarGen(EntityPlayer player, TileSolarGen tile) {
        super(player, tile);

        this.addSlotToContainer(new SlotIFItem(tile, TileSolarGen.SLOT_BATTERY, 42, 81));

        mapPlayerInventory();

        SlotGroup gInv = gRange(1, 1+36);
        SlotGroup gBattergy = gSlots(0);

        addTransferRule(gInv, IFItemManager.instance::isSupported, gBattergy);
        addTransferRule(gBattergy, gInv);
    }

}

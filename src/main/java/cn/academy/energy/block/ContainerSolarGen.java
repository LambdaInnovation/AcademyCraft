package cn.academy.energy.block;

import cn.lambdalib.template.container.CleanContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

public class ContainerSolarGen extends CleanContainer {

    public final EntityPlayer player;
    public final TileSolarGen tile;

    public ContainerSolarGen(EntityPlayer player, TileSolarGen tile) {
        this.player = player;
        this.tile = tile;

        this.addSlotToContainer(new SlotIFItem(tile, TileSolarGen.SLOT_BATTERY, 26, 71));

        InventoryPlayer inv = player.inventory;
        int STEP = 18;

        for(int i = 0; i < 9; ++i) {
            addSlotToContainer(new Slot(inv, i, -10 + i * STEP, 153));
        }

        for(int i = 1; i < 4; ++i) {
            for(int j = 0; j < 9; ++j) {
                int slot = (4 - i) * 9 + j;
                addSlotToContainer(new Slot(inv, slot, -10 + j * STEP, 149 - i * STEP));
            }
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile.getDistanceFrom(player.posX, player.posY, player.posZ) < 64;
    }
}

package cn.academy.core.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.tileentity.TileEntity;

public class TechUIContainer<T extends TileEntity> extends CleanContainer {

    public final EntityPlayer player;
    public final T tile;

    public TechUIContainer(EntityPlayer _player, T _tile) {
        player = _player;
        tile = _tile;
    }

    protected void mapPlayerInventory() {
        int STEP = 18;
        InventoryPlayer inv = player.inventory;

        for(int i = 0; i < 9; ++i) {
            addSlotToContainer(new Slot(inv, i, 6 + i * STEP, 163));
        }

        for(int i = 1; i < 4; ++i) {
            for(int j = 0; j < 9; ++j) {
                int slot = (4 - i) * 9 + j;
                addSlotToContainer(new Slot(inv, slot, 6 + j * STEP, 159 - i * STEP));
            }
        }
    }

    @Override
    public final boolean canInteractWith(EntityPlayer player) {
        return player.getDistanceSq(tile.x, tile.y, tile.z) < 64 && !tile.isInvalid();
    }
}

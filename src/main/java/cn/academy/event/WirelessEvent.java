package cn.academy.event;

import cn.academy.energy.api.block.IWirelessTile;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Base class of any wireless event.
 * All WirelessEvent should only be executed in SERVER side.
 * @author WeathFolD
 */
public class WirelessEvent extends Event {

    public final IWirelessTile tile;

    public WirelessEvent(IWirelessTile _tile) {
        tile = _tile;
    }

    public TileEntity getTileEntity() {
        return (TileEntity) tile;
    }

    public World getWorld() {
        return getTileEntity().getWorld();
    }

}
package cn.academy.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Fire this event to determine whether a block is permissible to be destroyed
 * depending on current world and player's permissions.
 * Cancelled if it shouldn't be.
 * This event is internally listened by BukkitAdapter.
 * @author EAirPeter
 */
@Cancelable
public final class BlockDestroyEvent extends Event {

    public final World world;
    public final EntityPlayer player;
    public final BlockPos pos;

    public BlockDestroyEvent(World world_, EntityPlayer player_,
                             BlockPos pos_)
    {
        world = world_;
        player = player_;
        pos = pos_;
    }

    public BlockDestroyEvent(World world_, BlockPos pos) {
        this(world_, null, pos);
    }
    
    public BlockDestroyEvent(EntityPlayer player_, BlockPos pos) {
        this(player_.world, player_, pos);
    }

}
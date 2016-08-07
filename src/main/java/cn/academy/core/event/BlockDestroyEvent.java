/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.event;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

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
    public final int x, y, z;

    public BlockDestroyEvent(World world_, EntityPlayer player_,
                             int x_, int y_, int z_)
    {
        world = world_;
        player = player_;
        x = x_;
        y = y_;
        z = z_;
    }

    public BlockDestroyEvent(World world_, int x_, int y_, int z_) {
        this(world_, null, x_, y_, z_);
    }
    
    public BlockDestroyEvent(EntityPlayer player_, int x_, int y_, int z_) {
        this(player_.worldObj, player_, x_, y_, z_);
    }

}

package cn.academy.core.event;

import net.minecraft.world.World;
import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;

/**
 * Fired whenever a block is to be destroyed. Canceled if this block shouldn't be.
 * This is listened by AC internal adapters to support residence and block destroy configs.
 * @author EAirPeter
 */
@Cancelable
public final class BlockDestroyEvent extends Event {

	public final World world;
	public final int x, y, z;
	
	public BlockDestroyEvent(World pWorld, int pX, int pY, int pZ) {
		world = pWorld;
		x = pX;
		y = pY;
		z = pZ;
	}
}

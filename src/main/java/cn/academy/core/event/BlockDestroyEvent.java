package cn.academy.core.event;

import net.minecraft.world.World;
import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;

@Cancelable
public final class BlockDestroyEvent extends Event {

	public final World world;
	public final int x, y, z;
	
	BlockDestroyEvent(World pWorld, int pX, int pY, int pZ) {
		world = pWorld;
		x = pX;
		y = pY;
		z = pZ;
	}
}

/**
 * 
 */
package cn.academy.core.energy;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.api.energy.IWirelessTile;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;

/**
 * Basic handling class of all wireless tiles.
 * @author WeathFolD
 */
@RegistrationClass
@RegEventHandler(Bus.FML)
public class WirelessSystem {
	
	private static final WirelessSystem INSTANCE = new WirelessSystem();
	
	Map<Integer, WiWorldData> worldData = new HashMap<Integer, WiWorldData>();

	/**
	 * If tile is a wireless channel, create a new channel. (Throws a runtime exception 
	 * if channel already exists); Else, add the tile into the existing channel.
	 */
	public static void registerTile(TileEntity tile, String channel) {
		assert(tile instanceof IWirelessTile);
		instance().getData(tile.getWorldObj()).register((IWirelessTile) tile, channel);
	}
	
	/**
	 * Unregister the tile from the current channel it is in.
	 * @param tile
	 */
	public static void unregisterTile(TileEntity tile) {
		assert(tile instanceof IWirelessTile);
		instance().getData(tile.getWorldObj()).unregister((IWirelessTile) tile);
	}
	
	private WiWorldData getData(World world) {
		return worldData.get(world.provider.dimensionId);
	}
	
	public static WirelessSystem instance() {
		return INSTANCE;
	}
	
	private WirelessSystem() {}

}

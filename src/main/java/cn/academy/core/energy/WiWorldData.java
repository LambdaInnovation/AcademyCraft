/**
 * 
 */
package cn.academy.core.energy;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.World;
import cn.academy.api.energy.IWirelessTile;

/**
 * Per world wireless-system data.
 * @author WeathFolD
 */
public class WiWorldData {

	private Map<String, WirelessNetwork> netMap = new HashMap();
	private Map<IWirelessTile, String> lookup = new HashMap();

	public WiWorldData(World world) {
	}
	
	public void register(IWirelessTile tile, String channel) {
		
	}
	
	public void unregister(IWirelessTile tile) {
		
	}
	
	public void onTick() {}

}

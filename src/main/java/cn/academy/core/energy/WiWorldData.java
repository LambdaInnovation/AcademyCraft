/**
 * 
 */
package cn.academy.core.energy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.world.World;
import cn.academy.api.energy.IWirelessGenerator;
import cn.academy.api.energy.IWirelessNode;
import cn.academy.api.energy.IWirelessReceiver;
import cn.academy.api.energy.IWirelessTile;
import cn.academy.core.AcademyCraft;

/**
 * Per world wireless-system data.
 * @author WeathFolD
 */
public class WiWorldData {
	
	public final World world;

	private Map<String, WirelessNetwork> netMap = new HashMap();
	private Map<IWirelessTile, String> lookup = new HashMap();

	public WiWorldData(World _world) {
		world = _world;
	}
	
	public void registerUser(IWirelessTile tile, IWirelessNode node) {
		String chan = lookup.get(node);
		if(chan == null) {
			AcademyCraft.log.error("The attached node " + node + "isn't in a wireless network");
			return;
		}
		WirelessNetwork net = netMap.get(chan);
		
		if(tile instanceof IWirelessGenerator) {
			net.registerGenerator((IWirelessGenerator) tile, node);
		} else if(tile instanceof IWirelessReceiver) {
			net.registerReceiver((IWirelessReceiver) tile, node);
		} else {
			throw new RuntimeException("Invalid register wireless user type: " + tile);
		}
		
		lookup.put(tile, chan);
	}
	
	public void registerNode(IWirelessNode node, String channel) {
		WirelessNetwork net = netMap.get(channel);
		if(net == null) { //Create a new network with this ID
			net = new WirelessNetwork(channel, world);
			netMap.put(channel, net);
		}
		
		net.registerNode(node);
		lookup.put(node, channel);
	}
	
	public void unregister(IWirelessTile tile) {
		String chan = lookup.remove(tile);
		if(chan == null) {
			AcademyCraft.log.error("Trying to unregister a non-present tile " + tile);
		}
		WirelessNetwork net = netMap.get(chan);
		net.unregister(tile);
	}
	
	public void onTick() {
		Iterator<Map.Entry<String, WirelessNetwork>> iter = netMap.entrySet().iterator();
		while(iter.hasNext()) {
			WirelessNetwork net = iter.next().getValue();
			if(net.isAlive()) {
				net.onTick();
			} else {
				iter.remove();
			}
		}
	}

}

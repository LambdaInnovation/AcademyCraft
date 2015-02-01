/**
 * 
 */
package cn.academy.core.energy;

import java.util.HashSet;
import java.util.Set;

import cn.academy.api.energy.IWirelessChannel;
import cn.academy.api.energy.IWirelessGenerator;
import cn.academy.api.energy.IWirelessNode;
import cn.academy.api.energy.IWirelessReceiver;

/**
 * A set of wireless tiles within the same channel.
 * @author WeathFolD
 */
public class WirelessNetwork {
	
	IWirelessChannel channel;
	
	Set<IWirelessNode> nodes = new HashSet<IWirelessNode>();
	Set<IWirelessReceiver> receivers = new HashSet<IWirelessReceiver>();
	Set<IWirelessGenerator> generators = new HashSet<IWirelessGenerator>();

	public WirelessNetwork() {}
	
	public void onTick() {
		
	}

}

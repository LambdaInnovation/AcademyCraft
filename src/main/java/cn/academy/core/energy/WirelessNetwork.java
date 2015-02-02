/**
 * 
 */
package cn.academy.core.energy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.World;
import cn.academy.api.energy.IWirelessGenerator;
import cn.academy.api.energy.IWirelessNode;
import cn.academy.api.energy.IWirelessReceiver;
import cn.academy.api.energy.IWirelessTile;

/**
 * A set of wireless tiles within the same channel.
 * @author WeathFolD
 */
public class WirelessNetwork {
	
	static final int MAX_LAG = 2000;
	
	double energyLag = 0.0; //Previous unprocessed energy for balancing, absval < MAX_LAG
	
	final String channel;
	final World world;
	
	Set<IWirelessNode> nodes = new HashSet();
	
	Map<IWirelessNode, NodeConns> conns = new HashMap();
	Map<IWirelessTile, IWirelessNode> lookup = new HashMap();
	
	public WirelessNetwork(String _channel, World _world) {
		channel = _channel;
		world = _world;
	}
	
	public boolean isAlive() {
		return !nodes.isEmpty();
	}
	
	public boolean hasNode(IWirelessNode node) {
		return nodes.contains(node);
	}
	
	public boolean hasUser(IWirelessTile tile) {
		IWirelessNode attach = lookup.get(tile);
		if(attach == null)
			return false;
		NodeConns conns = this.getConns(attach);
		if(tile instanceof IWirelessGenerator) {
			return conns.generators.contains(tile);
		} else if(tile instanceof IWirelessReceiver) {
			return conns.receivers.contains(tile);
		}
		return false;
	}
	
	public void registerNode(IWirelessNode node) {
		nodes.add(node);
	}
	
	public void registerGenerator(IWirelessGenerator gen, IWirelessNode node) {
		getConns(node).generators.add(gen);
		lookup.put(gen, node);
	}
	
	public void unregister(IWirelessTile tile) {
		if(tile instanceof IWirelessNode) {
			nodes.remove(tile);
			conns.remove(tile);
			Iterator<Map.Entry<IWirelessTile, IWirelessNode>> iter = 
					lookup.entrySet().iterator();
			while(iter.hasNext()) {
				Map.Entry<IWirelessTile, IWirelessNode> ent = iter.next();
				if(ent.getValue() == tile)
					iter.remove();
			}
			
			return;
		}
		
		IWirelessNode node = lookup.remove(tile);
		if(node != null) {
			NodeConns conns = getConns(node);
			if(tile instanceof IWirelessGenerator) {
				conns.generators.remove(tile);
			} else {
				conns.receivers.remove(tile);
			}
		}
	}
	
	public void registerReceiver(IWirelessReceiver rec, IWirelessNode node) {
		getConns(node).receivers.add(rec);
		lookup.put(rec, node);
	}
	
	public void onTick() {
		calcNodes();
		balanceNodes();
	}
	
	private void calcNodes() {
		for(Map.Entry<IWirelessNode, NodeConns> ent : conns.entrySet()) {
			calcNode(ent.getKey(), ent.getValue());
		}
	}
	
	private void calcNode(IWirelessNode node, NodeConns data) {
		double max = node.getMaxEnergy();
		double energy = node.getEnergy();
		
		Iterator<IWirelessGenerator> itgen = data.generators.iterator();
		Iterator<IWirelessReceiver> itrec = data.receivers.iterator();
		
		//Iterate gen and rec simultaneously
		while(itgen.hasNext() || itrec.hasNext()) {
			IWirelessGenerator gen = null;
			IWirelessReceiver rec = null;
			if(itgen.hasNext()) gen = itgen.next();
			if(itrec.hasNext()) rec = itrec.next();
			if(gen != null) {
				double req = max - energy;
				energy += gen.getOutput(req);
			}
			if(rec != null) {
				double req = Math.min(rec.getLatency(), rec.getEnergyRequired());
				req = Math.min(energy, req);
				energy -= req;
				rec.injectEnergy(req);
			}
		}
		
		node.setEnergy(energy);
	}
	
	/**
	 * 
	 */
	private void balanceNodes() {
		if(nodes.size() <= 1)
			return;
		double sum = 0.0;
		
		//First pass: calc sum
		for(IWirelessNode node : nodes) {
			sum += node.getEnergy();
		}
		sum += energyLag; //Also account unprocessed energy
		sum = Math.max(sum, 0);
		sum /= nodes.size();
		
		//Second pass: do the balance, use energyLag as middle buffer
		for(IWirelessNode node : nodes) {
			double delta = Math.min(sum, node.getMaxEnergy()) - node.getEnergy();
			delta = queryLag(Math.signum(delta) * Math.min(delta, node.getLatency()));
			node.setEnergy(delta + node.getEnergy());
		}
	}
	
	private double queryLag(double need) {
		if(energyLag + need > MAX_LAG) {
			energyLag = MAX_LAG;
			return MAX_LAG - energyLag;
		}
		if(energyLag + need < -MAX_LAG) {
			energyLag = -MAX_LAG;
			return -energyLag - MAX_LAG;
		}
		energyLag += need;
		return need;
	}
	
	private NodeConns getConns(IWirelessNode node) {
		NodeConns res = conns.get(node);
		if(res == null) {
			res = new NodeConns();
			conns.put(node, res);
		}
		return res;
	}
	
	static class NodeConns {
		public static Set<IWirelessGenerator> generators = new HashSet();
		public static Set<IWirelessReceiver> receivers = new HashSet();
	}

}

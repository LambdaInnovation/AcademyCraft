/**
 * 
 */
package cn.academy.core.energy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
	
	static final int BUFFER_SIZE = 5000;
	
	final String channel;
	final World world;
	
	List<IWirelessNode> nodes = new LinkedList();
	
	Map<IWirelessNode, NodeConns> conns = new HashMap();
	Map<IWirelessTile, IWirelessNode> lookup = new HashMap();
	
	public WirelessNetwork(String _channel, World _world) {
		channel = _channel;
		world = _world;
	}
	
	public boolean isAlive() {
		return !nodes.isEmpty();
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
		
		sum /= nodes.size();
		
		//Second pass: use buf as middle to balance
		Iterator<IWirelessNode> iter = nodes.iterator();
		IWirelessNode buf = iter.next();
		while(iter.hasNext()) {
			IWirelessNode node = iter.next();
			double delta = sum - node.getEnergy();
			delta = Math.signum(delta) * Math.min(Math.abs(delta), node.getLatency());
			if(delta > 0 && buf.getEnergy() - delta < 0) {
				delta = buf.getEnergy();
			}
			if(delta < 0 && buf.getEnergy() + delta < buf.getMaxEnergy()) {
				delta = buf.getEnergy() - buf.getMaxEnergy();
			}
			node.setEnergy(buf.getEnergy() + delta);
			buf.setEnergy(buf.getEnergy() - delta);
		}
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
		public static List<IWirelessGenerator> generators = new LinkedList();
		public static List<IWirelessReceiver> receivers = new LinkedList();
	}

}

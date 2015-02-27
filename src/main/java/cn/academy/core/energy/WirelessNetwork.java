/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.energy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.api.energy.IWirelessGenerator;
import cn.academy.api.energy.IWirelessNode;
import cn.academy.api.energy.IWirelessReceiver;
import cn.academy.api.energy.IWirelessTile;

/**
 * A set of wireless tiles within the same channel.
 * @author WeathFolD
 */
class WirelessNetwork {
	
	static final int MAX_LAG = 2000;
	
	boolean dead = false;
	
	double energyLag = 0.0; //Previous unprocessed energy for balancing, absval < MAX_LAG
	
	final String channel;
	final World world;
	
	String password;
	
	Set<IWirelessNode> nodes = new HashSet();
	
	Map<IWirelessNode, NodeConns> conns = new HashMap();
	Map<IWirelessTile, IWirelessNode> lookup = new HashMap();
	
	public WirelessNetwork(String _channel, World _world) {
		channel = _channel;
		world = _world;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String np) {
		System.out.println(channel + "- setpw: " + np);
		password = np;
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
	
	public IWirelessNode getConn(IWirelessTile tile) {
		return lookup.get(tile);
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
	
	public void destroy() {
		dead = true;
	}
	
	public void registerReceiver(IWirelessReceiver rec, IWirelessNode node) {
		getConns(node).receivers.add(rec);
		lookup.put(rec, node);
	}
	
	public void onTick() {
		Iterator<IWirelessNode> iter = nodes.iterator();
		while(iter.hasNext()) {
			IWirelessNode node = iter.next();
			TileEntity te = (TileEntity) node;
			if(te.isInvalid()) {
				iter.remove();
			}
		}
		if(nodes.size() == 0) {
			dead = true;
		}
		calcNodes();
		balanceNodes();
	}
	
	private void calcNodes() {
		for(Map.Entry<IWirelessNode, NodeConns> ent : conns.entrySet()) {
			calcNode(ent.getKey(), ent.getValue());
			//System.out.println(ent.getKey());
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
		sum += energyLag; //Also account for unprocessed energy
		sum = Math.max(sum, 0);
		sum /= nodes.size();
		
		//Second pass: do the balance, use energyLag as middle buffer
		for(IWirelessNode node : nodes) {
			double delta = Math.min(sum, node.getMaxEnergy()) - node.getEnergy();
			delta = queryLag(Math.signum(delta) * Math.min(Math.abs(delta), node.getLatency()));
			node.setEnergy(delta + node.getEnergy());
		}
	}
	
	private double queryLag(double need) {
		if(energyLag - need > MAX_LAG) {
			double preLag = energyLag;
			energyLag = MAX_LAG;
			return preLag - MAX_LAG;
		}
		if(energyLag - need < -MAX_LAG) {
			double preLag = energyLag;
			energyLag = -MAX_LAG;
			return preLag + MAX_LAG;
		}
		energyLag -= need;
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
		public Set<IWirelessGenerator> generators = new HashSet();
		public Set<IWirelessReceiver> receivers = new HashSet();
	}

}

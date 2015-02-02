/**
 * 
 */
package cn.academy.core.energy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.api.energy.IWirelessGenerator;
import cn.academy.api.energy.IWirelessNode;
import cn.academy.api.energy.IWirelessReceiver;
import cn.academy.api.energy.IWirelessTile;
import cn.academy.core.AcademyCraft;
import cn.liutils.util.GenericUtils;

/**
 * Per world wireless-system data.
 * @author WeathFolD
 */
public class WiWorldData {
	
	public final World world;

	private Map<String, WirelessNetwork> netMap = new HashMap();
	private Map<IWirelessTile, String> lookup = new WeakHashMap();
	private Map<Long, List<IWirelessNode>> chunkPos = new HashMap();

	public WiWorldData(World _world) {
		world = _world;
		if(_world.isRemote) {
			throw new RuntimeException("Trying to create a wireless net in client side. "
				+ "Don't register anything in client side!");
		}
	}
	
	public boolean isInChannel(IWirelessTile tile, String channel) {
		WirelessNetwork net = netMap.get(channel);
		if(net == null)
			return false;
		return tile instanceof IWirelessNode ? 
			net.hasNode((IWirelessNode) tile) : net.hasUser(tile);
	}
	
	public IWirelessNode getNearestNode(int x, int y, int z) {
		IWirelessNode ret = null;
		double minDist = Double.MAX_VALUE;
		for(int i = x - 1; i <= x + 1; ++i) {
			for(int j = z - 1; j <= z + 1; ++j) {
				
				IWirelessNode node = getNearestNodeInChunk(i, j, x, y, z);
				if(node == null) continue;
				TileEntity te = (TileEntity) node;
				double dsq = GenericUtils.distanceSq(x, y, z, te.xCoord, te.yCoord, te.zCoord);
				if(dsq < minDist) {
					minDist = dsq;
					ret = node;
				}
				
			}
		}
		return ret;
	}
	
	public List<IWirelessNode> getNodesIn(int x, int y, int z, double range, int max) {
		Set<String> excl = new HashSet();
		List<IWirelessNode> ret = new ArrayList();
		range *= range;
		for(int i = x - 1; i <= x + 1; ++i) {
			for(int j = z - 1; j <= z + 1; ++j) {
				if(ret.size() == max) break;
				ret.addAll(getNodesInChunk(excl, i, j, x, y, z, range, max - ret.size()));
			}
		}
		return ret;
	}
	
	private Set<IWirelessNode> getNodesInChunk(Set<String> excl, int cx, int cz, int x, int y, int z, double rsq, int max) {
		List<IWirelessNode> nodes = getNodeList(cx, cz);
		Set<IWirelessNode> ret = new HashSet();
		for(IWirelessNode node : nodes) {
			String chan = lookup.get(node);
			if(excl.contains(chan))
				continue;
			TileEntity tile = (TileEntity) node;
			double td = GenericUtils.distanceSq(tile.xCoord, tile.yCoord, tile.zCoord, x, y, z);
			if(td < rsq) {
				excl.add(chan);
				ret.add(node);
				if(max == ret.size())
					break;
			}
		}
		return ret;
	}
	
	private IWirelessNode getNearestNodeInChunk(int cx, int cz, int x, int y, int z) {
		List<IWirelessNode> nodes = getNodeList(cx, cz);
		double minDist = Double.MAX_VALUE;
		IWirelessNode res = null;
		for(IWirelessNode node : nodes) {
			TileEntity te = (TileEntity) node;
			double dsq = GenericUtils.distanceSq(x, y, z, te.xCoord, te.yCoord, te.zCoord);
			if(dsq < minDist) {
				minDist = dsq;
				res = node;
			}
		}
		return res;
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
		
		TileEntity tile = (TileEntity) node;
		net.registerNode(node);
		lookup.put(node, channel);
		getNodeList(tile.xCoord, tile.zCoord).add(node);
	}
	
	public void unregister(IWirelessTile tile) {
		String chan = lookup.remove(tile);
		if(chan == null) {
			AcademyCraft.log.error("Trying to unregister a non-present tile " + tile);
		}
		WirelessNetwork net = netMap.get(chan);
		net.unregister(tile);
		
		if(tile instanceof IWirelessNode) {
			TileEntity t = (TileEntity) tile;
			getNodeList(t.xCoord, t.zCoord).remove(tile);
		}
	}
	
	private List<IWirelessNode> getNodeList(int x, int z) {
		long key = getChunkKey(x, z);
		List<IWirelessNode> res = chunkPos.get(key);
		if(res == null) {
			res = new LinkedList<IWirelessNode>();
			chunkPos.put(key, res);
		}
		return res;
	}
	
	private long getChunkKey(int x, int z) {
		return x >> 4 + (((long)z) >> 4) << 28;
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

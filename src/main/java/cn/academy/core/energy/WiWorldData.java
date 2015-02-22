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
import cn.academy.core.energy.WirelessNetwork.NodeConns;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.misc.Pair;

/**
 * Per world wireless-system data.
 * @author WeathFolD
 */
class WiWorldData {
	
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
	
	public boolean hasChannel(String channel) {
		return netMap.containsKey(channel);
	}
	
	public boolean isInChannel(IWirelessTile tile, String channel) {
		WirelessNetwork net = netMap.get(channel);
		if(net == null)
			return false;
		return tile instanceof IWirelessNode ? 
			net.hasNode((IWirelessNode) tile) : net.hasUser(tile);
	}
	
	public void removeChannel(String chn) {
		WirelessNetwork wn = netMap.get(chn);
		if(wn != null)
			wn.dead = true;
	}
	
	public boolean isRegistered(IWirelessTile tile) {
		return lookup.containsKey(tile);
	}
	
	public IWirelessNode getConnectedNode(IWirelessTile tile) {
		String cn = lookup.get(tile);
		if(cn == null)
			return null;
		WirelessNetwork net = netMap.get(cn);
		return net.getConn(tile);
	}
	
	public String getChannel(IWirelessTile tile) {
		return lookup.get(tile);
	}
	
	public String getPassword(String chan) {
		WirelessNetwork wn = netMap.get(chan);
		return wn == null ? null : wn.getPassword();
	}
	
	public void setPassword(String chan, String pwd) {
		netMap.get(chan).setPassword(pwd);
	}
	
	public List<String> getChannelsIn(int x, int y, int z, double range, int max) {
		Set<String> excl = new HashSet();
		Set<String> ret = new HashSet();
		range *= range;
		
		int cx = x >> 4, cz = z >> 4;
		for(int i = cx - 1; i <= cx + 1; ++i) {
			for(int j = cz - 1; j <= cz + 1; ++j) {
				if(ret.size() == max) break;
				ret.addAll(getChannelsInChunk(excl, i, j, x, y, z, range, max - ret.size()));
			}
		}
		return new ArrayList<String>(ret);
	}
	
	public List<Pair<IWirelessNode, String>> getNodesIn(int x, int y, int z, double range, int max) {
		Set<String> excl = new HashSet();
		Set<Pair<IWirelessNode, String>> ret = new HashSet();
		range *= range;
		
		int cx = x >> 4, cz = z >> 4;
		for(int i = cx - 1; i <= cx + 1; ++i) {
			for(int j = cz - 1; j <= cz + 1; ++j) {
				if(ret.size() == max) break;
				ret.addAll(getNodesInChunk(excl, i, j, x, y, z, range, max - ret.size()));
			}
		}
		return new ArrayList<Pair<IWirelessNode, String>>(ret);
	}
	
	private Set<String> getChannelsInChunk(Set<String> excl, int cx, int cz, int x, int y, int z, double rsq, int max) {
		List<IWirelessNode> nodes = getNodeList(rawGetChunkKey(cx, cz));
		Set<String> ret = new HashSet();
		for(IWirelessNode node : nodes) {
			String chan = lookup.get(node);
			if(excl.contains(chan))
				continue;
			TileEntity tile = (TileEntity) node;
			double td = GenericUtils.distanceSq(tile.xCoord, tile.yCoord, tile.zCoord, x, y, z);
			System.out.println(tile);
			if(td < rsq) {
				excl.add(chan);
				ret.add(lookup.get(node));
				if(max == ret.size())
					break;
			}
		}
		return ret;
	}
	
	private Set<Pair<IWirelessNode, String>> getNodesInChunk(Set<String> excl, int cx, int cz, int x, int y, int z, double rsq, int max) {
		List<IWirelessNode> nodes = getNodeList(rawGetChunkKey(cx, cz));
		Set<Pair<IWirelessNode, String>> ret = new HashSet();
		for(IWirelessNode node : nodes) {
			String chan = lookup.get(node);
			if(excl.contains(chan))
				continue;
			TileEntity tile = (TileEntity) node;
			double td = GenericUtils.distanceSq(tile.xCoord, tile.yCoord, tile.zCoord, x, y, z);
			if(td < rsq) {
				excl.add(chan);
				ret.add(new Pair(node, lookup.get(node)));
				if(max == ret.size())
					break;
			}
		}
		return ret;
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
		getNodeList(getChunkKey(tile.xCoord, tile.zCoord)).add(node);
	}
	
	public void unregister(IWirelessTile tile) {
		String chan = lookup.remove(tile);
		if(chan == null) {
			//AcademyCraft.log.error("Trying to unregister a non-present tile " + tile);
			return;
		}
		WirelessNetwork net = netMap.get(chan);
		if(net == null)
			return;
		net.unregister(tile);
		
		if(tile instanceof IWirelessNode) {
			TileEntity t = (TileEntity) tile;
			getNodeList(getChunkKey(t.xCoord, t.zCoord)).remove(tile);
		}
	}
	
	private List<IWirelessNode> getNodeList(long key) {
		List<IWirelessNode> res = chunkPos.get(key);
		if(res == null) {
			res = new LinkedList<IWirelessNode>();
			chunkPos.put(key, res);
		}
		return res;
	}
	
	private List<IWirelessNode> getNodeList(TileEntity te) {
		return getNodeList(getChunkKey(te.xCoord, te.zCoord));
	}
	
	private long getChunkKey(int x, int z) {
		return rawGetChunkKey(x >> 4, z >> 4);
	}
	
	private long rawGetChunkKey(int x, int z) {
		return x + ((long)z << 28);
	}
	
	public void onTick() {
		Iterator<Map.Entry<String, WirelessNetwork>> iter = netMap.entrySet().iterator();
		while(iter.hasNext()) {
			WirelessNetwork net = iter.next().getValue();
			if(net.dead) {
				for(NodeConns conn : net.conns.values()) {
					for(IWirelessGenerator gen : conn.generators) {
						lookup.remove(gen);
					}
					for(IWirelessReceiver rec : conn.receivers) {
						lookup.remove(rec);
					}
				}
				for(IWirelessNode node : net.nodes) {
					getNodeList((TileEntity) node).remove(node);
					lookup.remove(node);
				}
				iter.remove();
			} else {
				net.onTick();
			}
		}
		
	}

}

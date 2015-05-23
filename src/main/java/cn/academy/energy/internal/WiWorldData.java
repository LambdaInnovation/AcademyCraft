/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import cn.academy.core.Debug;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.energy.internal.VBlocks.VWMatrix;
import cn.academy.energy.internal.VBlocks.VWNode;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.space.BlockPos;
import cn.liutils.util.space.IBlockFilter;

/**
 * @author WeAthFolD
 *
 */
public class WiWorldData extends WorldSavedData {
	
	public static final String ID = "AC_WEN";
	
	//Set by get method, which should be the ONLY way to access WiWorldData
	World world;

	public WiWorldData(String www) {
		super(ID);
	}
	
	//-----WEN-----
	
	private IBlockFilter filterWirelessBlocks = new IBlockFilter() {

		@Override
		public boolean accepts(World world, Block block, int x, int y, int z) {
			TileEntity te = world.getTileEntity(x, y, z);
			return te instanceof IWirelessMatrix || te instanceof IWirelessNode;
		}
		
	};
	
	/***
	 * Object valid for lookup: VWMatrix, VWNode, String ssid
	 */
	Map<Object, WirelessNet> networks = new HashMap();
	
	/**
	 * Internal, used to prevent concurrent modification.
	 */
	private List<WirelessNet> toRemove = new ArrayList();
	
	private void tickNetwork() {
		for(WirelessNet net : toRemove) {
			this.doRemoveNetwork(net);
		}
		toRemove.clear();
		
		Iterator<WirelessNet> iter = networks.values().iterator();
		while(iter.hasNext()) {
			WirelessNet net = iter.next();
			if(net.isDisposed()) {
				toRemove.add(net);
			} else {
				net.world = world;
				net.tick();
			}
		}
	}
	
	boolean createNetwork(IWirelessMatrix matrix, String ssid, String password) {
		if(networks.containsKey(ssid)) { //Doesn't allow ssid duplication
			return false;
		}
		
		// Kill old net of the same matrix, if any
		VWMatrix vm = new VWMatrix(matrix);
		if(networks.containsKey(vm)) {
			WirelessNet old = networks.get(vm);
			doRemoveNetwork(old);
		}
		
		//Add new
		WirelessNet net = new WirelessNet(this, vm, ssid, password);
		doAddNetwork(net);
		
		return true;
	}
	
	public Collection<WirelessNet> rangeSearch(double x, double y, double z, double range, int max) {
		AxisAlignedBB aabb = 
			AxisAlignedBB.getBoundingBox(
				x - range,
				y - range,
				z - range,
				x + range,
				y + range,
				z + range
			);
		Collection<BlockPos> bps = GenericUtils.getBlocksWithinAABB(world, aabb, filterWirelessBlocks, max * 2);
		
		Set<WirelessNet> set = new HashSet();
		for(BlockPos bp : bps) {
			TileEntity te = bp.getTileEntity(world);
			WirelessNet net;
			if(te instanceof IWirelessMatrix) {
				net = getNetwork((IWirelessMatrix) te);
			} else if(te instanceof IWirelessNode) {
				net = getNetwork((IWirelessNode) te);
			} else {
				throw new RuntimeException("Invalid TileEntity");
			}
			if(net != null) {
				set.add(net);
				if(set.size() >= max)
					return set;
			}
		}
		return set;
	}
	
	public WirelessNet getNetwork(String ssid) {
		return networks.get(ssid);
	}
	
	public WirelessNet getNetwork(IWirelessMatrix matrix) {
		System.out.println(matrix);
		return networks.get(new VWMatrix(matrix));
	}
	
	public WirelessNet getNetwork(IWirelessNode node) {
		return networks.get(new VWNode(node));
	}
	
	private void doRemoveNetwork(WirelessNet net) {
		Debug.print("DoRemoveNet" + net.ssid);
		net.onCleanup(this);
	}
	
	private void doAddNetwork(WirelessNet net) {
		net.onCreate(this);
	}
	
	private void loadNetwork(NBTTagCompound tag) {
		NBTTagList list = (NBTTagList) tag.getTag("networks");
		for(int i = 0; i < list.tagCount(); ++i) {
			NBTTagCompound tag2 = list.getCompoundTagAt(i);
			WirelessNet net = new WirelessNet(this, tag2);
			doAddNetwork(net);
		}
		Debug.print("WEN: Loaded " + list.tagCount() + " nets in " + world);
		
	}
	
	private void saveNetwork(NBTTagCompound tag) {
		NBTTagList list = new NBTTagList();
		Set<WirelessNet> added = new HashSet();
		for(WirelessNet net : networks.values()) {
			if(!added.contains(net) && !net.isDisposed()) {
				list.appendTag(net.toNBT());
			}
			added.add(net);
		}
		tag.setTag("networks", list);
	}
	
	//-----NodeConn----
	//TODO: Implement
	
	public NodeConnection getNodeConnection(IWirelessNode node) {
		return new NodeConnection();
	}
	
	public NodeConnection getNodeConnection(IWirelessUser user) {
		return new NodeConnection();
	}
	
	private void tickNode() {}
	
	private void loadNode(NBTTagCompound tag) {}
	
	private void saveNode(NBTTagCompound tag) {}
	
	//-----Generic-----
	public void tick() {
		this.markDirty();
		
		tickNetwork();
		tickNode();
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		NBTTagCompound tag1 = (NBTTagCompound) tag.getTag("net");
		if(tag1 != null)
			loadNetwork(tag1);
		
		tag1 = (NBTTagCompound) tag.getTag("node");
		if(tag1 != null)
			loadNode(tag1);
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		NBTTagCompound tag1 = new NBTTagCompound();
		saveNetwork(tag1);
		tag.setTag("net", tag1);
		
		tag1 = new NBTTagCompound();
		saveNode(tag1);
		tag.setTag("node", tag1);
	}
	
	public static WiWorldData get(World world) {
		if(world.isRemote) {
			throw new RuntimeException("Not allowed to create WiWorldData in client");
		}
		WiWorldData ret = (WiWorldData) world.loadItemData(WiWorldData.class, WiWorldData.ID);
		if(ret == null) {
			world.setItemData(ID, ret = new WiWorldData(ID));
		}
		ret.world = world;
		return ret;
	}
	
	private class ChunkCoord {
		int cx, cz;
		
		public ChunkCoord(int _cx, int _cz) {
			cx = _cx;
			cz = _cz;
		}
		
		public boolean isLoaded() {
			return world.getChunkProvider().chunkExists(cx, cz);
		}
		
		@Override
		public int hashCode() {
			return cx ^ cz;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj instanceof ChunkCoord) {
				ChunkCoord cc = (ChunkCoord) obj;
				return cc.cx == cx && cc.cz == cz;
			}
			return false;
		}
	}

}

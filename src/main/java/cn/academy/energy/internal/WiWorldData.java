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

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.energy.internal.VBlocks.VBlock;

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
	
	Map<Object, WirelessNet> networks = new WeakHashMap();
	Map<ChunkCoord, VBlock> posLookup = new HashMap();
	
	private void tickNetwork() {}
	
	boolean createNetwork(IWirelessMatrix matrix, String ssid, String password) {
		return false;
	}
	
	public WirelessNet getNetwork(String ssid) {
		return null;
	}
	
	public WirelessNet getNetwork(IWirelessMatrix matrix) {
		return null;
	}
	
	public WirelessNet getNetwork(IWirelessNode node) {
		return null;
	}
	
	private void loadNetwork(NBTTagCompound tag) {}
	
	private void saveNetwork(NBTTagCompound tag) {}
	
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

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
package cn.academy.energy.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.energy.api.block.IWirelessGenerator;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessReceiver;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.energy.internal.NodeConn;
import cn.academy.energy.internal.WiWorldData;
import cn.academy.energy.internal.WirelessNet;
import cn.lambdalib.util.helper.BlockPos;
import cn.lambdalib.util.mc.IBlockSelector;
import cn.lambdalib.util.mc.WorldUtils;

/**
 * All kinds of funcs about wireless system.
 * @author WeAthFolD
 */
public class WirelessHelper {

	//-----WirelessNetwork
	
	public static WirelessNet getWirelessNet(World world, String ssid) {
		return WiWorldData.get(world).getNetwork(ssid);
	}
	
	public static WirelessNet getWirelessNet(IWirelessMatrix matrix) {
		TileEntity tile = (TileEntity) matrix;
		return WiWorldData.get(tile.getWorldObj()).getNetwork(matrix);
	}
	
	public static WirelessNet getWirelessNet(IWirelessNode node) {
		TileEntity tile = (TileEntity) node;
		return WiWorldData.get(tile.getWorldObj()).getNetwork(node);
	}
	
	/**
	 * @return Whether the wireless node is linked into a WEN
	 */
	public static boolean isNodeLinked(IWirelessNode node) {
		return getWirelessNet(node) != null;
	}
	
	/**
	 * @return Whether the matrix is initialized with an SSID
	 */
	public static boolean isMatrixActive(IWirelessMatrix matrix) {
		return getWirelessNet(matrix) != null;
	}
	
	/**
	 * Get a list of WirelessNet at the position within the given range.
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @param range
	 * @param max
	 * @return
	 */
	public static Collection<WirelessNet> getNetInRange(World world, int x, int y, int z, double range, int max) {
		WiWorldData data = WiWorldData.get(world);
		return data.rangeSearch(x, y, z, range, max);
	}
	
	//-----Node Connection
	public static NodeConn getNodeConn(IWirelessNode node) {
		TileEntity tile = (TileEntity) node;
		return WiWorldData.get(tile.getWorldObj()).getNodeConnection(node);
	}
	
	public static NodeConn getNodeConn(IWirelessUser gen) {
		TileEntity tile = (TileEntity) gen;
		return WiWorldData.get(tile.getWorldObj()).getNodeConnection(gen);
	}
	
	public static boolean isReceiverLinked(IWirelessReceiver rec) {
		return getNodeConn(rec) != null;
	}
	
	public static boolean isGeneratorLinked(IWirelessGenerator gen) {
		return getNodeConn(gen) != null;
	}
	
	/**
	 * Get a list of IWirelessNode that can reach the given position.
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @return nodes in the area, does not guarantee any order
	 */
	public static List<IWirelessNode> getNodesInRange(World world, double x, double y, double z) {
		double range = 20.0;
		List<BlockPos> list = WorldUtils.getBlocksWithin(world, x, y, z, range, 100, new IBlockSelector() {

			@Override
			public boolean accepts(World world, int x, int y, int z, Block block) {
				TileEntity te = world.getTileEntity(x, y, z);
				return te instanceof IWirelessNode;
			}
			
		});
		
		List<IWirelessNode> ret = new ArrayList();
		for(BlockPos bp : list) {
			ret.add((IWirelessNode) bp.getTile());
		}
		
		return ret;
	}
	
}

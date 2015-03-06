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
package cn.academy.core.energy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import cn.academy.api.energy.IWirelessNode;
import cn.academy.api.energy.IWirelessTile;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cn.liutils.util.misc.Pair;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;

/**
 * Basic handling class of all wireless tiles.
 * @author WeathFolD
 */
@RegistrationClass
public class WirelessSystem {
	
	@RegEventHandler()
	public static final WirelessSystem INSTANCE = new WirelessSystem();
	
	static Map<Integer, WiWorldData> worldData = new HashMap<Integer, WiWorldData>();
	
	public static boolean hasNetwork(World world, String id) {
		return getData(world).hasChannel(id);
	}
	
	/**
	 * Link a IWirelessGenerator or IWirelessReceiver to a (usually nearest) node.
	 * @param tile
	 * @param node
	 */
	public static void attachTile(IWirelessTile tile, IWirelessNode node) {
		getData(((TileEntity)tile).getWorldObj()).registerUser(tile, node);
	}

	/**
	 * Register an wireless node into the network.
	 */
	public static void registerNode(IWirelessNode node, String channel) {
		assert(node instanceof TileEntity);
		getData(((TileEntity) node).getWorldObj()).registerNode(node, channel);
	}
	
	public static String getPassword(World world, String channel) {
		return getData(world).getPassword(channel);
	}
	
	public static void setPassword(World world, String channel, String pwd) {
		getData(world).setPassword(channel, pwd);
	}
	
	/**
	 * Unregister an user tile from the current channel it is in.
	 * @param tile
	 */
	public static void unregisterTile(IWirelessTile tile) {
		assert(tile instanceof TileEntity);
		instance();
		WirelessSystem.getData(((TileEntity) tile).getWorldObj()).unregister(tile);
	}
	
	public static void removeChannel(World world, String channel) {
		getData(world).removeChannel(channel);
	}
	
	public static boolean isTileIn(IWirelessTile tile, String channel) {
		return getData(((TileEntity)tile).getWorldObj()).isInChannel(tile, channel);
	}
	
	public static boolean isTileRegistered(IWirelessTile tile) {
		return getData(((TileEntity)tile).getWorldObj()).isRegistered(tile);
	}
	
	public static List<String> getAvailableChannels(World world, int x, int y, int z, double range, int max) {
		return getData(world).getChannelsIn(x, y, z, range, max);
	}
	
	public static List<Pair<IWirelessNode, String>> getAvailableNodes(World world, int x, int y, int z, double range, int max) {
		return getData(world).getNodesIn(x, y, z, range, max);
	}
	
	public static String getTileChannel(IWirelessTile tile) {
		return getData(((TileEntity)tile).getWorldObj()).getChannel(tile);
	}
	
	public static IWirelessNode getConnectedNode(IWirelessTile tile) {
		return getData(((TileEntity)tile).getWorldObj()).getConnectedNode(tile);
	}
	
	private static WiWorldData getData(World world) {
		WiWorldData res = worldData.get(world.provider.dimensionId);
		if(res == null) {
			res = new WiWorldData(world);
			worldData.put(world.provider.dimensionId, res);
		}
		return res;
	}
	
	public static WirelessSystem instance() {
		return INSTANCE;
	}
	
	@SubscribeEvent
	public void onServerTick(ServerTickEvent event) {
		for(WiWorldData data : worldData.values()) {
			data.onTick();
		}
	}
	
	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event) {
		if(!event.world.isRemote) {
			worldData.remove(event.world.provider.dimensionId);
			System.out.println("remOVE");
		}
	}
	
	@SubscribeEvent
	public void onWorldUnload(WorldEvent.Unload event) {
		worldData.remove(event.world.provider.dimensionId);
	}
	

}

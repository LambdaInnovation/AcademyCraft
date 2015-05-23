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

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import cn.academy.core.AcademyCraft;
import cn.academy.core.Debug;
import cn.academy.energy.api.event.ChangePassEvent;
import cn.academy.energy.api.event.CreateNetworkEvent;
import cn.academy.energy.api.event.DestroyNetworkEvent;
import cn.academy.energy.api.event.LinkNodeEvent;
import cn.academy.energy.api.event.UnlinkNodeEvent;
import cn.academy.energy.internal.VBlocks.VWNode;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;

/**
 * @author WeAthFolD
 *
 */
@Registrant
public class WirelessSystem {
	
	@RegEventHandler
	public static WirelessSystem INSTANCE = new WirelessSystem();
	
	private WirelessSystem() {
		AcademyCraft.log.info("AcademyCraft Wireless Energy System is loading...");
	}
	
	@SubscribeEvent
	public void onServerTick(ServerTickEvent event) {
		//System.out.println("---");
		for(WorldServer ws : MinecraftServer.getServer().worldServers) {
			if(ws.getClass() == WorldServer.class) //FIX: WorldServerMulti instance cause lookup loss.
				//TODO: Investigate why.
				WiWorldData.get(ws).tick();
		}
		//System.out.println("---");
	}
	
	@SubscribeEvent
	public void onCreateNet(CreateNetworkEvent event) {
		WiWorldData data = WiWorldData.get(event.getWorld());
		Debug.print(event.getWorld());
		if(!data.createNetwork(event.mat, event.ssid, event.pwd)) {
			event.setCanceled(true);
		}
	}
	
	@SubscribeEvent
	public void onDestroyNet(DestroyNetworkEvent event) {
		WiWorldData data = WiWorldData.get(event.getWorld());
		WirelessNet net = data.getNetwork(event.mat);
		if(net != null) net.dispose();
	}
	
	@SubscribeEvent
	public void changePass(ChangePassEvent event) {
		WiWorldData data = WiWorldData.get(event.getWorld());
		WirelessNet net = data.getNetwork(event.mat);
		if(net == null || !net.resetPassword(event.oldpwd, event.pwd)) {
			event.setCanceled(true);
		}
	}
	
	@SubscribeEvent
	public void linkNode(LinkNodeEvent event) {
		WiWorldData data = WiWorldData.get(event.getWorld());
		WirelessNet net = data.getNetwork(event.ssid);
		
		if(net == null || !net.addNode(new VWNode(event.node), event.pwd))
			event.setCanceled(true);
	}
	
	@SubscribeEvent
	public void unlinkNode(UnlinkNodeEvent event) {
		WiWorldData data = WiWorldData.get(event.getWorld());
		WirelessNet net = data.getNetwork(event.node);
		
		if(net != null)
			net.removeNode(new VWNode(event.node));
	}
	
}

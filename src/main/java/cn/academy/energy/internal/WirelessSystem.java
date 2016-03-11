/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.internal;

import cn.academy.core.AcademyCraft;
import cn.academy.energy.api.block.IWirelessGenerator;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessReceiver;
import cn.academy.energy.api.event.WirelessUserEvent.UserType;
import cn.academy.energy.api.event.node.LinkUserEvent;
import cn.academy.energy.api.event.node.UnlinkUserEvent;
import cn.academy.energy.api.event.wen.*;
import cn.academy.energy.internal.VBlocks.VNGenerator;
import cn.academy.energy.internal.VBlocks.VNReceiver;
import cn.academy.energy.internal.VBlocks.VWNode;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEventHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

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
        //System.out.println("{");
        for(WorldServer ws : MinecraftServer.getServer().worldServers) {
            //System.out.println(ws + "/" + ws.provider.dimensionId);
            //if(ws.getClass() != WorldServer.class) continue;
            WiWorldData data = WiWorldData.getNonCreate(ws);

            if(data != null) {
                data.tick();
            }
        }
        //System.out.println("}");
    }
    
    @SubscribeEvent
    public void onCreateNet(CreateNetworkEvent event) {
        WiWorldData data = WiWorldData.get(event.getWorld());
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
    
    @SubscribeEvent
    public void linkUser(LinkUserEvent event) {
        WiWorldData data = WiWorldData.get(event.getWorld());
        NodeConn conn = data.getNodeConnection(event.node);

        if (event.needAuth) {
            if (!event.node.getPassword().equals(event.password)) {
                event.setCanceled(true);
                return;
            }
        }
        
        if(event.type == UserType.GENERATOR) {
            if(!conn.addGenerator(new VNGenerator(event.getAsGenerator())))
                event.setCanceled(true);
        } else { //RECEIVER
            if(!conn.addReceiver(new VNReceiver(event.getAsReceiver())))
                event.setCanceled(true);
        }
    }
    
    @SubscribeEvent
    public void unlinkUser(UnlinkUserEvent event) {
        WiWorldData data = WiWorldData.get(event.getWorld());
        
        if(event.type == UserType.GENERATOR) {
            IWirelessGenerator gen = event.getAsGenerator();
            NodeConn conn = data.getNodeConnection(gen);
            if(conn != null) conn.removeGenerator(new VNGenerator(gen));
        } else { //RECEIVER
            IWirelessReceiver rec = event.getAsReceiver();
            NodeConn conn = data.getNodeConnection(rec);
            if(conn != null) conn.removeReceiver(new VNReceiver(rec));
        }
    }
    
    private void debug(Object msg) {
        AcademyCraft.log.info("WirelessSys: " + msg);
    }
    
}

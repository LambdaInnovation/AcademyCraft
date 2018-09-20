package cn.academy.energy.impl;

import cn.academy.energy.api.block.IWirelessGenerator;
import cn.academy.energy.api.block.IWirelessReceiver;
import cn.academy.event.WirelessUserEvent.UserType;
import cn.academy.event.energy.LinkUserEvent;
import cn.academy.event.energy.UnlinkUserEvent;
import cn.academy.event.energy.*;
import cn.academy.energy.impl.VBlocks.VNGenerator;
import cn.academy.energy.impl.VBlocks.VNReceiver;
import cn.academy.energy.impl.VBlocks.VWNode;
import cn.lambdalib2.registry.mc.RegEventHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

/**
 * @author WeAthFolD
 *
 */
public class WirelessSystem {
    
    @RegEventHandler
    public static WirelessSystem INSTANCE = new WirelessSystem();
    
    private WirelessSystem() {}
    
    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        if (event.phase == Phase.START)
            return;

        for(WorldServer ws : FMLCommonHandler.instance().getMinecraftServerInstance().worlds) {
            WiWorldData data = WiWorldData.getNonCreate(ws);

            if(data != null) {
                data.tick();
            }
        }
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
        if(net == null || !net.resetPassword(event.pwd)) {
            event.setCanceled(true);
        }
    }
    
    @SubscribeEvent
    public void linkNode(LinkNodeEvent event) {
        WiWorldData data = WiWorldData.get(event.getWorld());
        WirelessNet net = data.getNetwork(event.matrix);
        
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

}
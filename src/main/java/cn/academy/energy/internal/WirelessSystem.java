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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import cn.academy.core.AcademyCraft;
import cn.academy.energy.api.IWirelessGenerator;
import cn.academy.energy.api.IWirelessNode;
import cn.academy.energy.api.IWirelessReceiver;
import cn.academy.energy.api.IWirelessTile;
import cn.academy.energy.api.event.CreateNetworkEvent;
import cn.academy.energy.api.event.DestroyNetworkEvent;
import cn.academy.energy.api.event.LinkNodeEvent;
import cn.academy.energy.api.event.LinkUserEvent;
import cn.academy.energy.api.event.UnlinkNodeEvent;
import cn.academy.energy.api.event.UnlinkUserEvent;
import cn.academy.energy.api.event.WirelessUserEvent.UserType;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;

/**
 * @author WeathFolD
 */
@RegistrationClass
public class WirelessSystem {
    
    @RegEventHandler
    public static WirelessSystem instance = new WirelessSystem();
    
    Map<World, WiWorldData> table = new HashMap();

    WirelessSystem() {
    	AcademyCraft.log.info("AcademyCraft Wireless Energy System is loading.");
    }
    
    @SubscribeEvent
    public void worldLoaded(WorldEvent.Load event) {
        if(event.world.isRemote) return;
        WiWorldData data = (WiWorldData) event.world.loadItemData(WiWorldData.class, WiWorldData.ID);
        data.world = event.world;
        if(data != null) {
            table.put(event.world, data);
        }
    }
    
    @SubscribeEvent
    public void worldSaved(WorldEvent.Save event) {
        if(event.world.isRemote) return;
        WiWorldData data = table.get(event.world);
        if(data != null) {
            event.world.setItemData(WiWorldData.ID, data);
        }
    }
    
    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        for(WiWorldData data : table.values()) {
            data.tick();
        }
    }
    
    @SubscribeEvent
    public void linkUser(LinkUserEvent event) {
        if(event.type == UserType.GENERATOR) {
            getDataFor(event.getWorld()).linkGenerator(
                new Coord(event.tile, BlockType.GENERATOR), 
                new Coord(event.tile, BlockType.NODE));
        } else if(event.type == UserType.RECEIVER) {
            getDataFor(event.getWorld()).linkReceiver(
                    new Coord(event.tile, BlockType.GENERATOR), 
                    new Coord(event.tile, BlockType.NODE));
        }
    }
    
    @SubscribeEvent
    public void unlinkUser(UnlinkUserEvent event) {
        if(event.type == UserType.GENERATOR) {
            getDataFor(event.getWorld()).unlinkGenerator(new Coord(event.tile, BlockType.GENERATOR));
        } else if(event.type == UserType.RECEIVER) {
            getDataFor(event.getWorld()).unlinkGenerator(new Coord(event.tile, BlockType.RECEIVER));
        }
    }
    
    @SubscribeEvent
    public void createNetwork(CreateNetworkEvent event) {
        if(!getDataFor(event.getWorld()).createNetwork(new Coord(event.mat, BlockType.MATRIX)
                , event.ssid, event.isEncrypted, event.pwd))
            event.setCanceled(true);
    }
    
    @SubscribeEvent
    public void destroyNetwork(DestroyNetworkEvent event) {
        getDataFor(event.getWorld()).destroyNetwork(new Coord(event.mat, BlockType.MATRIX));
    }
    
    @SubscribeEvent
    public void linkNode(LinkNodeEvent event) {
        boolean ret = getDataFor(event.getWorld()).linkNode(event.ssid, new Coord(event.node, BlockType.NODE), event.pwd);
        event.setCanceled(!ret);
    }
    
    @SubscribeEvent
    public void unlinkNode(UnlinkNodeEvent event) {
        getDataFor(event.getWorld()).unlinkNode(new Coord(event.node, BlockType.NODE));
    }
    
    //Lookup part(Helper methods)
    /**
     * Return the ssid for the matrix or node, or null if they are not loaded.
     */
    public static String getSSID(IWirelessTile matOrNode) {
    	return instance.getDataFor(world(matOrNode)).getSSID(new Coord(matOrNode));
    }
    
    /**
     * Get all the available SSIDs for an wirelss node.
     */
    public static Collection<String> getAvailableSSIDs(IWirelessNode node) {
        return instance.getDataFor(world(node)).getAvailableSSIDs(node);
    }
    
    /**
     * Get all the available nodes for an wireless user(receiver/generator).
     */
    public static Collection<Coord> getAvailableNodes(IWirelessTile user) {
        return instance.getDataFor(world(user)).getAvailableNodes(user);
    }
    
    public static boolean isTileActive(IWirelessTile tile) {
    	WiWorldData data = instance.getDataFor(world(tile));
    	if(tile instanceof IWirelessGenerator || tile instanceof IWirelessReceiver) {
    		return data.isUserPresent(new Coord(tile));
    	} else {
    		return data.isMatrixOrNodePresent(new Coord(tile));
    	}
    }
    
    //Internal Implementation
    private static World world(IWirelessTile te) {
        return ((TileEntity)te).getWorldObj();
    }
    
    private WiWorldData getDataFor(World world) {
        WiWorldData ret = table.get(world);
        if(ret == null) {
            ret = new WiWorldData();
            world.setItemData(WiWorldData.ID, ret);
            table.put(world, ret);
        }
        
        ret.world = world;
        
        return ret;
    }

}

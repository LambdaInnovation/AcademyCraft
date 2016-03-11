/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.client.gui;

import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessTile;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.energy.api.event.node.LinkUserEvent;
import cn.academy.energy.api.event.node.UnlinkUserEvent;
import cn.academy.energy.internal.NodeConn;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.Future;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Data;
import cn.lambdalib.networkcall.s11n.StorageOption.Instance;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;

/**
 * @author WeAthFolD
 */
@Registrant
public class LinkToNodeSyncs {
    
    @RegNetworkCall(side = Side.SERVER)
    public static void retrieveNearbyNetworks(@Instance TileEntity te, @Instance Future future) {
        if(te instanceof IWirelessUser) {
            IWirelessUser user = (IWirelessUser) te;
            future.setAndSync(WirelessHelper.getNodesInRange(te.getWorldObj(),
                    te.xCoord, te.yCoord, te.zCoord));
        } else {
            future.setAndSync(new ArrayList());
        }
    }
    
    @RegNetworkCall(side = Side.SERVER)
    public static void retrieveCurrentLink(@Instance TileEntity te, @Data Future future) {
        if(te instanceof IWirelessUser) {
            IWirelessUser user = (IWirelessUser) te;
            NodeConn conn = WirelessHelper.getNodeConn(user);
            IWirelessNode node = conn == null ? null : conn.getNode();
            
            future.setAndSync(node == null ? "" : node.getNodeName());
        } else {
            future.setAndSync("");
        }
    }
    
    @RegNetworkCall(side = Side.SERVER)
    public static void startLink(@Instance TileEntity te, @Instance TileEntity node, @Data Future future) {

    }
    
    @RegNetworkCall(side = Side.SERVER)
    public static void disconnect(@Instance TileEntity te) {
        if(te instanceof IWirelessUser) {
            MinecraftForge.EVENT_BUS.post(new UnlinkUserEvent((IWirelessTile) te));
        }
    }
    
}

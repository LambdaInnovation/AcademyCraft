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
package cn.academy.energy.client.gui;

import java.util.ArrayList;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
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

/**
 * @author WeAthFolD
 */
@Registrant
public class LinkToNodeSyncs {
    
    @RegNetworkCall(side = Side.SERVER)
    public static void retrieveNearbyNetworks(@Instance TileEntity te, @Instance Future future) {
        if(te instanceof IWirelessUser) {
            IWirelessUser user = (IWirelessUser) te;
            future.setAndSync(WirelessHelper.getNodesInRange(te.getWorldObj(), te.xCoord + 0.5, te.yCoord + 0.5, te.zCoord + 0.5));
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
        if(te instanceof IWirelessUser && node instanceof IWirelessNode) {
            future.setAndSync(!MinecraftForge.EVENT_BUS.post(
                new LinkUserEvent((IWirelessUser) te, (IWirelessNode) node)));
        } else {
            future.setAndSync(false);
        }
    }
    
    @RegNetworkCall(side = Side.SERVER)
    public static void disconnect(@Instance TileEntity te) {
        if(te instanceof IWirelessUser) {
            MinecraftForge.EVENT_BUS.post(new UnlinkUserEvent((IWirelessTile) te));
        }
    }
    
}

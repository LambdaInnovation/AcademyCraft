/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.client.app;

import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.energy.api.event.node.LinkUserEvent;
import cn.academy.energy.api.event.wen.LinkNodeEvent;
import cn.academy.energy.internal.WirelessNet;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.Future;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Data;
import cn.lambdalib.networkcall.s11n.StorageOption.Instance;
import cpw.mods.fml.relauncher.Side;
import net.minecraftforge.common.MinecraftForge;

/**
 * @author WeAthFolD
 */
@Registrant
public class Syncs {
    
    @RegNetworkCall(side = Side.SERVER)
    static void querySSID(@Instance IWirelessMatrix matrix, @Data Future future) {
        WirelessNet net = WirelessHelper.getWirelessNet(matrix);
        future.setAndSync(net != null ? net.getSSID() : null);
    }

    @RegNetworkCall(side = Side.SERVER)
    static void authorizeMatrix(@Instance IWirelessMatrix matrix, @Data String password, @Data Future future) {
        WirelessNet net = WirelessHelper.getWirelessNet(matrix);
        future.setAndSync(net != null && net.getPassword().equals(password));
    }
    
    @RegNetworkCall(side = Side.SERVER)
    static void linkNodeToMatrix(@Instance IWirelessNode node, @Instance IWirelessMatrix matrix, @Data String password, @Data Future future) {
        WirelessNet net = WirelessHelper.getWirelessNet(matrix);
        future.setAndSync(net == null ? false :
                !MinecraftForge.EVENT_BUS.post(new LinkNodeEvent(node, net.getSSID(), password)));
    }
    
    @RegNetworkCall(side = Side.SERVER)
    static void linkUserToNode(@Instance IWirelessUser user, @Instance IWirelessNode node, @Data Future future) {
        future.setAndSync(!MinecraftForge.EVENT_BUS.post(new LinkUserEvent(user, node)));
    }
    
}

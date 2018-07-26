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
import cn.lambdalib2.annoreg.core.Registrant;
import cn.lambdalib2.s11n.network.Future;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkS11n.NetworkS11nType;
import cpw.mods.fml.relauncher.Side;
import net.minecraftforge.common.MinecraftForge;

/**
 * @author WeAthFolD
 */
@Registrant
@NetworkS11nType
public class Syncs {

    private static final String
            MSG_QUERY_SSID = "query_ssid",
            MSG_AUTH_MATRIX = "auth_matrix",
            MSG_AUTH_NODE   = "auth_node",
            MSG_LINK_NODE = "link_node",
            MSG_LINK_USER = "link_user";

    private static Object delegate = NetworkMessage.staticCaller(Syncs.class);

    static void querySSID(IWirelessMatrix matrix, Future<String> future) {
        send(MSG_QUERY_SSID, matrix, future);
    }

    static void authorizeMatrix(IWirelessMatrix matrix, String password, Future<Boolean> future) {
        send(MSG_AUTH_MATRIX, matrix, password, future);
    }

    static void authorizeNode(IWirelessNode node, String password, Future<Boolean> future) {
        send(MSG_AUTH_NODE, node, password, future);
    }

    static void linkNodeToMatrix(IWirelessNode node, IWirelessMatrix matrix, String password, Future<Boolean> future) {
        send(MSG_LINK_NODE, node, matrix, password, future);
    }

    static void linkUserToNode(IWirelessUser user, IWirelessNode node, Future<Boolean> future) {
        send(MSG_LINK_USER, user, node, future);
    }

    @NetworkMessage.Listener(channel=MSG_QUERY_SSID, side=Side.SERVER)
    static void hQuerySSID(IWirelessMatrix matrix, Future<String> future) {
        WirelessNet net = WirelessHelper.getWirelessNet(matrix);
        future.sendResult(net != null ? net.getSSID() : null);
    }

    @NetworkMessage.Listener(channel=MSG_AUTH_MATRIX, side=Side.SERVER)
    static void hAuthorizeMatrix(IWirelessMatrix matrix, String password, Future<Boolean> future) {
        WirelessNet net = WirelessHelper.getWirelessNet(matrix);
        future.sendResult(net != null && net.getPassword().equals(password));
    }

    @NetworkMessage.Listener(channel=MSG_LINK_NODE, side=Side.SERVER)
    static void hLinkNodeToMatrix(IWirelessNode node, IWirelessMatrix matrix, String password, Future<Boolean> future) {
        WirelessNet net = WirelessHelper.getWirelessNet(matrix);
        future.sendResult(net != null &&
                !MinecraftForge.EVENT_BUS.post(new LinkNodeEvent(node, net.getMatrix(), password)));
    }

    @NetworkMessage.Listener(channel=MSG_LINK_USER, side=Side.SERVER)
    static void hLinkUserToNode(IWirelessUser user, IWirelessNode node, Future<Boolean> future) {
        future.sendResult(!MinecraftForge.EVENT_BUS.post(new LinkUserEvent(user, node)));
    }

    @NetworkMessage.Listener(channel=MSG_AUTH_NODE, side=Side.SERVER)
    static void hAuthNode(IWirelessNode node, String pass, Future<Boolean> future) {
        future.sendResult(node.getPassword().equals(pass));
    }

    private static void send(String channel, Object... args) {
        NetworkMessage.sendToServer(delegate, channel, args);
    }
    
}

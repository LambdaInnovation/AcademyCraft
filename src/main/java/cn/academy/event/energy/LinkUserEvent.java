package cn.academy.event.energy;

import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessTile;
import cn.academy.event.WirelessUserEvent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

/**
 * Fired whenever a wireless user(receiver or generator) is to be linked to a node.
 * Canceled if not correctly linked.
 * @author WeathFolD
 */
@Cancelable
public class LinkUserEvent extends WirelessUserEvent {

    public final IWirelessNode node;
    public final String password;
    public final boolean needAuth;

    public LinkUserEvent(IWirelessTile _tile, IWirelessNode _node, String _password) {
        super(_tile);
        node = _node;
        password = _password;
        needAuth = true;
    }

    public LinkUserEvent(IWirelessTile _tile, IWirelessNode _node) {
        super(_tile);
        node = _node;
        password = "invalid";
        needAuth = false;
    }

}
package cn.academy.energy.api.event.wen;

import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessTile;
import cn.academy.energy.api.event.WirelessEvent;

/**
 * Fired when a node is to be unlinked.
 * @author WeathFolD
 */
public class UnlinkNodeEvent extends WirelessEvent {
    
    public final IWirelessNode node;

    public UnlinkNodeEvent(IWirelessTile _node) {
        super(_node);
        node = (IWirelessNode) _node;
    }

}
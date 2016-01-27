/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.api.event.node;

import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessTile;
import cn.academy.energy.api.event.WirelessUserEvent;
import cpw.mods.fml.common.eventhandler.Cancelable;

/**
 * Fired whenever a wireless user(receiver or generator) is to be linked to a node.
 * Canceled if not correctly linked.
 * @author WeathFolD
 */
@Cancelable
public class LinkUserEvent extends WirelessUserEvent {

    public final IWirelessNode node;
    
    public LinkUserEvent(IWirelessTile _tile, IWirelessNode _node) {
        super(_tile);
        node = _node;
    }

}

/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.api.event.wen;

import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.event.WirelessEvent;
import cpw.mods.fml.common.eventhandler.Cancelable;

/**
 * Fired when a node is to be linked.
 * @author WeathFolD
 */
@Cancelable
public class LinkNodeEvent extends WirelessEvent {
    
    public final IWirelessNode node;
    public final IWirelessMatrix matrix;
    public final String pwd;
    
    public LinkNodeEvent(IWirelessNode _node, IWirelessMatrix _matrix) {
        this(_node, _matrix, "");
    }

    public LinkNodeEvent(IWirelessNode _node, IWirelessMatrix _matrix, String _pwd) {
        super(_node);
        node = _node;
        matrix = _matrix;
        pwd = _pwd;
    }

}

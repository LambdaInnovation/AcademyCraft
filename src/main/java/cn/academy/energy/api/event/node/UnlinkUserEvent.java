/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.api.event.node;

import cn.academy.energy.api.block.IWirelessTile;
import cn.academy.energy.api.event.WirelessUserEvent;

/**
 * Fired whenever a wireless user is to be destroyed.</br>
 * NOTE: When a user is no longer available, it will be automatically unlinked.
 * @author WeathFolD
 */
public class UnlinkUserEvent extends WirelessUserEvent {

    public UnlinkUserEvent(IWirelessTile _tile) {
        super(_tile);
    }

}

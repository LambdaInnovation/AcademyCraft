/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * This project is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.api.event;

import cn.academy.energy.api.IWirelessNode;
import cn.academy.energy.api.IWirelessTile;

/**
 * Fired whenever a wireless user(receiver or generator) is to be linked to a node.
 * @author WeathFolD
 */
public class LinkUserEvent extends WirelessUserEvent {

    public final IWirelessNode node;
    
    public LinkUserEvent(IWirelessTile _tile, IWirelessNode _node) {
        super(_tile);
        node = _node;
    }

}

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
package cn.academy.energy.api.event.wen;

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
    public final String ssid;
    public final String pwd;
    
    public LinkNodeEvent(IWirelessNode _node, String _ssid) {
        this(_node, _ssid, "");
    }
    
    /**
     * @param _tile
     */
    public LinkNodeEvent(IWirelessNode _node, String _ssid, String _pwd) {
        super(_node);
        node = _node;
        ssid = _ssid;
        pwd = _pwd;
    }

}

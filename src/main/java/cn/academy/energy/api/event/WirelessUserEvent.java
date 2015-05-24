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
package cn.academy.energy.api.event;

import cn.academy.energy.api.block.IWirelessGenerator;
import cn.academy.energy.api.block.IWirelessReceiver;
import cn.academy.energy.api.block.IWirelessTile;

/**
 * @author WeathFolD
 *
 */
public class WirelessUserEvent extends WirelessEvent {
    
    public enum UserType { GENERATOR, RECEIVER }
    
    public final UserType type;

    public WirelessUserEvent(IWirelessTile _tile) {
        super(_tile);
        if(_tile instanceof IWirelessGenerator) {
            type = UserType.GENERATOR;
        } else if(_tile instanceof IWirelessReceiver){
            type = UserType.RECEIVER;
        } else {
            throw new RuntimeException("Invalid user type");
        }
    }
    
    public IWirelessGenerator getAsGenerator() {
    	return (IWirelessGenerator) tile;
    }
    
    public IWirelessReceiver getAsReceiver() {
    	return (IWirelessReceiver) tile;
    }

}

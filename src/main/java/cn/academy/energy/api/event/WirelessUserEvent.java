/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
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

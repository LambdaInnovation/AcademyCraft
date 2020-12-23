package cn.academy.event;

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
package cn.academy.event.energy;

import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.event.WirelessEvent;

/**
 * Fire this whenever you want to destroy a wireless network manually.
 * NOTE: If the tile is no longer available, it will be removed automatically.
 * @author WeathFolD
 */
public class DestroyNetworkEvent extends WirelessEvent {
    
    public final IWirelessMatrix mat;
    
    public DestroyNetworkEvent(IWirelessMatrix _mat) {
       super(_mat);
       mat = _mat;
    }

}
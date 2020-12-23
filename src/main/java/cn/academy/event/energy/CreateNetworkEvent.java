package cn.academy.event.energy;

import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.event.WirelessEvent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

/**
 * Fired whenever you want to create an wireless network.
 * @author WeathFolD
 */
@Cancelable
public class CreateNetworkEvent extends WirelessEvent {

    public final IWirelessMatrix mat;
    public final String ssid;
    public final String pwd;

    /**
     * Encrypted creation
     */
    public CreateNetworkEvent(IWirelessMatrix _mat, String _ssid, String _pwd) {
        super(_mat);
        mat = _mat;
        ssid = _ssid;
        pwd = _pwd;
    }

}
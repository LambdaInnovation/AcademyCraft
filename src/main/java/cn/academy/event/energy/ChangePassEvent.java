package cn.academy.event.energy;

import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.event.WirelessEvent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

@Cancelable
public class ChangePassEvent extends WirelessEvent {

    public final IWirelessMatrix mat;
    public final String pwd;

    /**
     * Encrypted creation
     */
    public ChangePassEvent(IWirelessMatrix _mat, String _pwd) {
        super(_mat);
        mat = _mat;
        pwd = _pwd;
    }

}
package cn.academy.energy.api.event.wen;

import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.event.WirelessEvent;
import cpw.mods.fml.common.eventhandler.Cancelable;

@Cancelable
public class ChangePassEvent extends WirelessEvent {

    public final IWirelessMatrix mat;
    public final String oldpwd; //Can be null if not encrypted.
    public final String pwd;
    
    /**
     * Encrypted creation
     */
    public ChangePassEvent(IWirelessMatrix _mat, String _oldpwd, String _pwd) {
        super(_mat);
        mat = _mat;
        oldpwd = _oldpwd;
        pwd = _pwd;
    }
    
}

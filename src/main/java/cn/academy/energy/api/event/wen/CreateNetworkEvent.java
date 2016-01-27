/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.api.event.wen;

import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.event.WirelessEvent;
import cpw.mods.fml.common.eventhandler.Cancelable;

/**
 * Fired whenever you want to create an wireless network.
 * @author WeathFolD
 */
@Cancelable
public class CreateNetworkEvent extends WirelessEvent {

    public final IWirelessMatrix mat;
    public final boolean isEncrypted;
    public final String ssid;
    public final String pwd;
    
    /**
     * Non-encryption creation
     */
    public CreateNetworkEvent(IWirelessMatrix _mat, String _ssid) {
        super(_mat);
        mat = _mat;
        ssid = _ssid;
        isEncrypted = false;
        pwd = "";
    }
    
    /**
     * Encrypted creation
     */
    public CreateNetworkEvent(IWirelessMatrix _mat, String _ssid, String _pwd) {
        super(_mat);
        mat = _mat;
        ssid = _ssid;
        isEncrypted = true;
        pwd = _pwd;
    }
    
}

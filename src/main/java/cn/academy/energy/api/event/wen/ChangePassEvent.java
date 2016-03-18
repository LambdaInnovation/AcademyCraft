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

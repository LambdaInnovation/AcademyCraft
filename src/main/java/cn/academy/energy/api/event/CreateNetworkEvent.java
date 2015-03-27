/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * This project is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.api.event;

import net.minecraft.tileentity.TileEntity;
import cn.academy.energy.api.IWirelessMatrix;
import cpw.mods.fml.common.eventhandler.Event;

/**
 * Fired whenever you want to create an wireless network.
 * @author WeathFolD
 */
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

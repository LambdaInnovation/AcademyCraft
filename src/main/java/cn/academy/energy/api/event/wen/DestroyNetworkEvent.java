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
package cn.academy.energy.api.event.wen;

import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.event.WirelessEvent;

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

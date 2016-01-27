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
package cn.academy.terminal.event;

import cn.academy.terminal.App;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Fired in both client and server when any app is installed, disregarding pre-installed apps.
 * @author WeAthFolD
 */
public class AppInstalledEvent extends Event {
    
    public final EntityPlayer player;
    public final App app;
    
    public AppInstalledEvent(EntityPlayer _player, App _app) {
        player = _player;
        app = _app;
    }
    
}

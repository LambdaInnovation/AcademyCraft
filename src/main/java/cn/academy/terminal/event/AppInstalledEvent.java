/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
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

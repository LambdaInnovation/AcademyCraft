/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.terminal.event;

import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Fired both at client and server when player installed data terminal.
 * @author WeAthFolD
 */
public class TerminalInstalledEvent extends Event {
    
    final EntityPlayer player;
    
    public TerminalInstalledEvent(EntityPlayer _player) {
        player = _player;
    }
    
}

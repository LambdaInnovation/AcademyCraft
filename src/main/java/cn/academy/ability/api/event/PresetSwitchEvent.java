/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.event;

import net.minecraft.entity.player.EntityPlayer;


/**
 * Fired only in client, when the player has just changed his preset using the
 * Preset Switch key(default 'C').
 * @author WeAthFolD
 */
public class PresetSwitchEvent extends AbilityEvent {

    public PresetSwitchEvent(EntityPlayer _player) {
        super(_player);
    }

}

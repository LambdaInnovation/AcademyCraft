/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.event;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Fired BOTH CLIENT AND SERVER. After player has updated his preset.
 * This event is used to indicate that a preset has been UPDATED. It is not necessarily
 * fired only when player edits his preset using UI, but also when data are syncedSingle from
 * server/client.
 * @author WeAthFolD
 */
public class PresetUpdateEvent extends AbilityEvent {

    public PresetUpdateEvent(EntityPlayer _player) {
        super(_player);
    }

}

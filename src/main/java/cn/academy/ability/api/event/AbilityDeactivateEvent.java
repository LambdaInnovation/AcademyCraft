/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.event;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Fired both CLIENT and SERVER, when player has deactivated
 * his/her ability using activate key ('V').
 * @author WeAthFolD
 */
public class AbilityDeactivateEvent extends AbilityEvent {

    public AbilityDeactivateEvent(EntityPlayer _player) {
        super(_player);
    }

}

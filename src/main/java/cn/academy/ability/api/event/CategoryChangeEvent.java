/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.event;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Fired in both sides, when the player category has just changed.
 * @author WeAthFolD
 */
public class CategoryChangeEvent extends AbilityEvent {

    public CategoryChangeEvent(EntityPlayer _player) {
        super(_player);
    }
    
}

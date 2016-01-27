/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.event;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Fired at server when player's ability level has changed.
 * @author WeAthFolD
 */
public class LevelChangeEvent extends AbilityEvent {
    
    public LevelChangeEvent(EntityPlayer p) {
        super(p);
    }
    
}

/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.event;

import cn.academy.ability.api.data.AbilityData;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.entity.player.EntityPlayer;

/**
 * @author WeAthFolD
 */
public abstract class AbilityEvent extends Event {
    
    public final EntityPlayer player;
    
    public AbilityEvent(EntityPlayer _player) {
        player = _player;
    }
    
    public AbilityData getAbilityData() {
        return AbilityData.get(player);
    }
    
}

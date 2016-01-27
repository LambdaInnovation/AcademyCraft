/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.event;

import cn.academy.ability.api.Skill;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Fired in SERVER only, when the specified skill's exp is being ADDED.
 * When change category or sth else the event won't get fired.
 * @author WeAthFolD
 */
public class SkillExpChangedEvent extends AbilityEvent {
    
    public final Skill skill;

    public SkillExpChangedEvent(EntityPlayer _player, Skill _skill) {
        super(_player);
        skill = _skill;
    }

}

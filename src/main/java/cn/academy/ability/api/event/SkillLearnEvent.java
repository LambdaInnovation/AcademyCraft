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
 * Fired in BOTH CLIENT AND SERVER, when an Skill is newly learned by the player.
 * @author WeAthFolD
 */
public class SkillLearnEvent extends AbilityEvent {
    
    public final Skill skill;

    public SkillLearnEvent(EntityPlayer player, Skill _skill) {
        super(player);
        skill = _skill;
    }

}

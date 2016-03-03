/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.generic.skill;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.event.CalcEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * Generic passive skill: Mind Training Course
 * @author WeAthFolD
 */
public class SkillMindCourse extends Skill {

    public SkillMindCourse() {
        super("mind_course", 5);
        this.canControl = false;
        this.isGeneric = true;
        
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void recalcCPRecover(CalcEvent.CPRecoverSpeed evt) {
        if (AbilityData.get(evt.player).isSkillLearned(this)) {
            evt.value *= 1.2f;
        }
    }

}

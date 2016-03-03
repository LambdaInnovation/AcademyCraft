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
 * Generic skill: Brain Course.
 * @author WeAthFolD
 */
public class SkillBrainCourse extends Skill {

    public SkillBrainCourse() {
        super("brain_course", 3);
        this.canControl = false;
        this.isGeneric = true;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void recalcMaxCP(CalcEvent.MaxCP event) {
        AbilityData adata = AbilityData.get(event.player);
        if (adata.isSkillLearned(this)) {
            event.value += 1000;
        }
    }

}

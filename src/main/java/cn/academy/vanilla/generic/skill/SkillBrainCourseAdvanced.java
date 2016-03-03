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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;

/**
 * Generic skill: Advanced Brain Course.
 * @author WeAthFolD
 */
public class SkillBrainCourseAdvanced extends Skill {

    public SkillBrainCourseAdvanced() {
        super("brain_course_advanced", 4);
        this.canControl = false;
        this.isGeneric = true;
        
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void recalcMaxCP(CalcEvent.MaxCP event) {
        if (learned(event.player)) {
            event.value += 1500;
        }
    }

    @SubscribeEvent
    public void recalcMaxOverload(CalcEvent.MaxOverload event) {
        if (learned(event.player)) {
            event.value += 100;
        }
    }

    private boolean learned(EntityPlayer player) {
        return AbilityData.get(player).isSkillLearned(this);
    }
}

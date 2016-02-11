/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.generic.skill;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.PipedValues;
import cn.academy.ability.api.event.SkillExpAddedEvent;
import cn.academy.core.config.ConfigEnv;
import cn.academy.core.config.PlayerConfigEnv.PlayerEnvRebuildEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * Generic skill: Advanced Brain Course.
 * @author WeAthFolD
 */
public class SkillBrainCourseAdvanced extends Skill {

    public SkillBrainCourseAdvanced() {
        super("brain_course_advanced", 5);
        this.canControl = false;
        this.isGeneric = true;
        
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    public void onExpAdded(SkillExpAddedEvent event) {
        AbilityData aData = event.getAbilityData();
        if(event.skill.canControl() && aData.isSkillLearned(this)) {
            event.getAbilityData().addSkillExp(this, event.amount * this.getFloat("incr_rate"));
        }
    }

    @SubscribeEvent
    public void onRebuildEnv(PlayerEnvRebuildEvent evt) {
        AbilityData aData = AbilityData.get(evt.entityPlayer);
        if (aData.isSkillLearned(this)) {
            evt.env.addFloatPipe(ConfigEnv.path(PipedValues.MAXCP), input -> input + 1500);
            evt.env.addFloatPipe(ConfigEnv.path(PipedValues.MAXOVERLOAD), input -> input + 100);
        }
    }

}

/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.generic.skill;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.event.SkillExpAddedEvent;
import cn.academy.core.util.SubscribePipeline;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
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
    
    @SubscribePipeline("ability.maxcp")
    public float addMaxCP(float maxcp, EntityPlayer player) {
        if(AbilityData.get(player).isSkillLearned(this))
            maxcp += 1500;
        return maxcp;
    }
    
    @SubscribePipeline("ability.maxo")
    public float addMaxO(float maxo, EntityPlayer player) {
        if(AbilityData.get(player).isSkillLearned(this))
            maxo += 100;
        return maxo;
    }
    
    @SubscribeEvent
    public void onExpAdded(SkillExpAddedEvent event) {
        AbilityData aData = event.getAbilityData();
        if(event.skill.canControl() && aData.isSkillLearned(this)) {
            event.getAbilityData().addSkillExp(this, event.amount * this.getFloat("incr_rate"));
        }
    }

}

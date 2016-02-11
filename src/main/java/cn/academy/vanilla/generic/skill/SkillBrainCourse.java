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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;

/**
 * Generic skill: Brain Course.
 * @author WeAthFolD
 */
public class SkillBrainCourse extends Skill {

    public SkillBrainCourse() {
        super("brain_course", 4);
        this.canControl = false;
        this.isGeneric = true;
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    public void onExpAdded(SkillExpAddedEvent event) {
        AbilityData aData = event.getAbilityData();
        if(event.skill.canControl() && aData.isSkillLearned(this)) {
            aData.addSkillExp(this, event.amount * this.getFloat("incr_rate"));
        }
    }

    @SubscribeEvent
    public void onEnvRebuild(PlayerEnvRebuildEvent evt) {
        AbilityData aData = AbilityData.get(evt.entityPlayer);
        if (aData.isSkillLearned(this)) {
            evt.env.addFloatPipe(ConfigEnv.path(PipedValues.MAXCP), cp -> cp + 1000);
        }
    }

}

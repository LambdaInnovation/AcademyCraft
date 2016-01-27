/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.teleporter.passiveskills;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.event.SkillExpAddedEvent;
import cn.academy.core.util.SubscribePipeline;
import cn.lambdalib.util.generic.MathUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;

/**
 * @author WeAthFolD
 */
public class SpaceFluctuation extends Skill {

    public static final SpaceFluctuation instance = new SpaceFluctuation();

    private SpaceFluctuation() {
        super("space_fluct", 4);
        this.canControl = false;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribePipeline("ac.teleporter.crit_prob.0")
    public float addProb0(float prob, EntityPlayer player) {
        AbilityData aData = AbilityData.get(player);
        if (aData.isSkillLearned(this)) {
            return prob + MathUtils.lerpf(0.01f, 0.03f, aData.getSkillExp(this));
        }
        return prob;
    }

    @SubscribePipeline("ac.teleporter.crit_prob.1")
    public float addCritProb1(float prob, EntityPlayer player) {
        AbilityData aData = AbilityData.get(player);
        if (aData.isSkillLearned(this)) {
            return prob + MathUtils.lerpf(0.10f, 0.15f, aData.getSkillExp(this));
        }
        return prob;
    }

    @SubscribePipeline("ac.teleporter.crit_prob.2")
    public float addCritProb2(float prob, EntityPlayer player) {
        AbilityData aData = AbilityData.get(player);
        if (aData.isSkillLearned(this)) {
            return prob + MathUtils.lerpf(0.18f, 0.25f, aData.getSkillExp(this));
        }
        return prob;
    }

    @SubscribeEvent
    public void onExpAdded(SkillExpAddedEvent event) {
        if (event.skill.canControl()) {
            event.getAbilityData().addSkillExp(this, event.amount * getFloat("incr_rate"));
        }
    }

}

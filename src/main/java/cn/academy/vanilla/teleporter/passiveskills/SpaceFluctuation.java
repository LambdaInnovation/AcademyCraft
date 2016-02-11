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
import cn.academy.core.config.ConfigEnv;
import cn.academy.core.config.PlayerConfigEnv.PlayerEnvRebuildEvent;
import cn.academy.vanilla.teleporter.TPPipes;
import cn.lambdalib.util.generic.MathUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
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

    @SubscribeEvent
    public void onExpAdded(SkillExpAddedEvent event) {
        if (event.skill.canControl() && AbilityData.get(event.player).isSkillLearned(this)) {
            event.getAbilityData().addSkillExp(this, event.amount * getFloat("incr_rate"));
        }
    }

    @SubscribeEvent
    public void onEnvRebuild(PlayerEnvRebuildEvent event) {
        AbilityData aData = AbilityData.get(event.entityPlayer);
        if (aData.isSkillLearned(this)) {
            event.env.addFloatPipe(ConfigEnv.path(TPPipes.pathCritProb(0)),
                    input -> input + MathUtils.lerpf(0.01f, 0.03f, aData.getSkillExp(this)));
            event.env.addFloatPipe(ConfigEnv.path(TPPipes.pathCritProb(1)),
                    input -> input + MathUtils.lerpf(0.10f, 0.15f, aData.getSkillExp(this)));
            event.env.addFloatPipe(ConfigEnv.path(TPPipes.pathCritProb(2)),
                    input -> input + MathUtils.lerpf(0.18f, 0.25f, aData.getSkillExp(this)));
        }
    }

}

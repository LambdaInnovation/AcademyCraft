/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.teleporter.passiveskills;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.PipedValues;
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
public class DimFoldingTheorem extends Skill {

    public static final DimFoldingTheorem instance = new DimFoldingTheorem();

    private DimFoldingTheorem() {
        super("dim_folding_theorem", 1);
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
    public void onEnvRebuild(PlayerEnvRebuildEvent evt) {
        AbilityData aData = AbilityData.get(evt.entityPlayer);
        if (aData.isSkillLearned(this)) {
            evt.env.addFloatPipe(ConfigEnv.path(TPPipes.pathCritProb(2)),
                    input -> input + MathUtils.lerpf(0.1f, 0.2f, aData.getSkillExp(this)));
            evt.env.addFloatPipe(ConfigEnv.path(PipedValues.ANY_DAMAGE), input -> input * 1.2f);
        }
    }

}

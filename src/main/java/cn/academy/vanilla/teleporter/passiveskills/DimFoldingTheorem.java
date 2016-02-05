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
public class DimFoldingTheorem extends Skill {

    public static final DimFoldingTheorem instance = new DimFoldingTheorem();

    private DimFoldingTheorem() {
        super("dim_folding_theoreom", 1);
        this.canControl = false;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribePipeline("teleporter.?.$damage")
    public float addDamage(float dmg, EntityPlayer player) {
        AbilityData aData = AbilityData.get(player);
        if (aData.isSkillLearned(this)) {
            return dmg * 1.2f;
        }
        return dmg;
    }

    @SubscribePipeline("ac.teleporter.crit_prob.2")
    public float addCritHitProbability(float prob, EntityPlayer player) {
        AbilityData aData = AbilityData.get(player);
        if (aData.isSkillLearned(this)) {
            return prob + MathUtils.lerpf(0.1f, 0.2f, aData.getSkillExp(this));
        }
        return prob;
    }

    @SubscribeEvent
    public void onExpAdded(SkillExpAddedEvent event) {
        if (event.skill.canControl() && AbilityData.get(event.player).isSkillLearned(this)) {
            event.getAbilityData().addSkillExp(this, event.amount * getFloat("incr_rate"));
        }
    }

}

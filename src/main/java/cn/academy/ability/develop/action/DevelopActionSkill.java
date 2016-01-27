/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.develop.action;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.client.skilltree.SkillTreeLocal;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.ability.develop.LearningHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * Learn a specific kind of skill.
 * @author WeAthFolD
 */
public class DevelopActionSkill implements IDevelopAction {
    
    Skill skill;

    public DevelopActionSkill(Skill _skill) {
        skill = _skill;
    }

    @Override
    public int getStimulations(EntityPlayer player) {
        return skill.getLearningStims();
    }

    @Override
    public void onLearned(EntityPlayer player) {
        AbilityData.get(player).learnSkill(skill);
    }

    @Override
    public boolean validate(EntityPlayer player, IDeveloper developer) {
        return LearningHelper.canLearn(AbilityData.get(player), developer, skill);
    }

    @Override
    public ResourceLocation getIcon(EntityPlayer player) {
        return skill.getHintIcon();
    }

    @Override
    public String getName(EntityPlayer player) {
        return SkillTreeLocal.learnSkill() + " " + skill.getDisplayName();
    }

}

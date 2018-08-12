package cn.academy.ability.develop.action;

import cn.academy.ability.Skill;
import cn.academy.datapart.AbilityData;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.ability.develop.LearningHelper;
import net.minecraft.entity.player.EntityPlayer;

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

}
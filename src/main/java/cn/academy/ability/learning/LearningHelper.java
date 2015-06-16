package cn.academy.ability.learning;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.api.AbilityData;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.learning.LearningCondition;

public class LearningHelper {

	/**
	 * Determine whether a player is capable of learning a specific skill.
	 */
	public static boolean canLearnSkill(EntityPlayer player, Skill skill) {
		AbilityData data = AbilityData.get(player);
		if(!data.getCategory().containsSkill(skill))
			return false;
		for(LearningCondition lc : skill.getLearningConditions()) {
			if(!lc.accepts(skill, data))
				return false;
		}
		return true;
	}
	
	public static boolean isSkillRootLearned(EntityPlayer player, Skill skill) {
		AbilityData data = AbilityData.get(player);
		if(!data.getCategory().containsSkill(skill))
			return false;
		return data.isSkillLearned(skill.getParent());
	}
	
}

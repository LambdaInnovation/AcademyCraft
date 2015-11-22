/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.develop;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.develop.action.IDevelopAction;
import cn.academy.ability.develop.condition.IDevCondition;
import net.minecraft.entity.player.EntityPlayer;

/**
 * All sorts of judging utilities about ability learning.
 * Available in both client and server.
 * @author WeAthFolD
 */
public class LearningHelper {
	
	/**
	 * @return Whether the given player can level up currently
	 */
	public static boolean canLevelUp(DeveloperType type, AbilityData aData) {
		Category c = aData.getCategory();
		if(c == null)
			return true;
		return CPData.get(aData.getEntity()).canLevelUp();
	}
	
	/**
	 * Skills that can be potentially learned will be displayed on the Skill Tree gui.
	 */
	public static boolean canBePotentiallyLearned(AbilityData data, Skill skill) {
		return data.getLevel() >= skill.getLevel() &&
				(!data.isSkillLearned(skill) && 
					(skill.getParent() == null || data.isSkillLearned(skill.getParent())));
	}
	
	/**
	 * @return Whether the given skill can be learned.
	 */
	public static boolean canLearn(AbilityData data, IDeveloper dev, Skill skill) {
		for(IDevCondition cond : skill.getDevConditions()) {
			if(!cond.accepts(data, dev, skill))
				return false;
		}
		return true;
	}

    public static double getEstimatedConsumption(EntityPlayer player, DeveloperType blktype, IDevelopAction type) {
        return blktype.getCPS() * type.getStimulations(player);
    }
	
}

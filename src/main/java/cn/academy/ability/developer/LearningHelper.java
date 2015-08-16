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
package cn.academy.ability.developer;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;

/**
 * All sorts of judging utilities about ability learning.
 * @author WeAthFolD
 */
public class LearningHelper {
	
	/**
	 * @return Whether the given player can level up currently
	 */
	public static boolean canLevelUp(AbilityData aData) {
		return true;
	}
	
	/**
	 * Skills that can be potentially learned will be displayed on the Skill Tree gui
	 */
	public static boolean canBePotentiallyLearned(AbilityData data, Skill skill) {
		return data.getLevel() >= skill.getLevel() &&
				(!data.isSkillLearned(skill) && 
					(skill.getParent() == null || data.isSkillLearned(skill.getParent())));
	}
	
}

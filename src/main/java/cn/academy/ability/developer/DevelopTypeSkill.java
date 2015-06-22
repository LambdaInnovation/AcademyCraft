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

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.api.AbilityData;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.learning.LearningHelper;

/**
 * Maybe the only IDevelopType. Learn a specific kind of skill.
 * @author WeAthFolD
 */
public class DevelopTypeSkill implements IDevelopType {
	
	Skill skill;

	public DevelopTypeSkill(Skill _skill) {
		skill = _skill;
	}

	@Override
	public int getStimulations() {
		return skill.getLearningStims();
	}

	@Override
	public void onLearned(EntityPlayer player) {
		AbilityData.get(player).learnSkill(skill);
	}

	@Override
	public boolean validate(EntityPlayer player) {
		return LearningHelper.canLearnSkill(player, skill);
	}

	@Override
	public DeveloperType getMinimumType() {
		return skill.getMinimumDeveloperType();
	}

}

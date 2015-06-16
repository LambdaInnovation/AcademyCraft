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
package cn.academy.ability.learning;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.api.AbilityData;
import cn.academy.ability.api.Skill;
import cn.academy.core.AcademyCraft;

/**
 * @author WeAthFolD
 *
 */
public class SkillDevProgress extends CommonDevProgress {
	
	final Skill skill;

	public SkillDevProgress(Skill _skill) {
		super(getLearningCost(_skill.getLevel()), (EntityPlayer player) -> {
			AbilityData data = AbilityData.get(player);
		});
		skill = _skill;
	}
	
	private static int getLearningCost(int skillLevel) {
		return AcademyCraft.getFunction("learning.learning_cost").callInteger(skillLevel);
	}

}

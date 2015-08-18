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

import net.minecraft.util.ResourceLocation;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;

/**
 * @author WeAthFolD
 */
public class DevConditionDep implements IDevCondition {
	
	public final Skill dependency;
	public final float requiredExp;
	
	public DevConditionDep(Skill _dep) {
		this(_dep, 0.0f);
	}
	
	public DevConditionDep(Skill _dep, float _requiredExp) {
		dependency = _dep;
		requiredExp = _requiredExp;
	}

	@Override
	public boolean accepts(AbilityData data, Skill skill) {
		return data.isSkillLearned(dependency) &&
				data.getSkillExp(dependency) >= requiredExp;
	}

	@Override
	public ResourceLocation getIcon() {
		return dependency.getHintIcon();
	}

	@Override
	public String getHintText() {
		return dependency.getDisplayName() + String.format(": %.0f%%", requiredExp * 100);
	}

}

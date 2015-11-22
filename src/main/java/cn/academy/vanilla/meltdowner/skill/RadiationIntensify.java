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
package cn.academy.vanilla.meltdowner.skill;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.lambdalib.util.generic.MathUtils;

/**
 * Just a placeholder. implementation is in MDDamageHelper.
 * @author WeAthFolD
 */
public class RadiationIntensify extends Skill {

	public static final RadiationIntensify instance = new RadiationIntensify();

	private RadiationIntensify() {
		super("rad_intensify", 1);
		this.canControl = false;
		this.expCustomized = true;
	}
	
	@Override
	public float getSkillExp(AbilityData data) {
		CPData cpData = CPData.get(data.getEntity());
		return MathUtils.clampf(0, 1, cpData.getMaxCP() / CPData.getInitCP(5));
	}

	public float getRate(AbilityData data) {
		return this.callFloatWithExp("rate", data);
	}
	
}

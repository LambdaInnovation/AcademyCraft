/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.meltdowner.skill;

/**
 * @author WeathFolD
 *
 */
public class SkillMiningBasic extends SkillMiningBase {

	/**
	 * 
	 */
	public SkillMiningBasic() {
		this.setLogo("meltdowner/mine_basic.png");
		this.setName("md_minebasic");
		setMaxLevel(15);
	}

	@Override
	float getConsume(int slv, int lv) {
		return 0.6f * (10 - slv * 0.3f - lv * 0.4f);
	}

	@Override
	int getHarvestLevel() {
		return 1;
	}

	@Override
	int getSpawnRate() {
		return 30;
	}
	
}

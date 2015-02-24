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
public class SkillMiningExpert extends SkillMiningBase {

	/**
	 * 
	 */
	public SkillMiningExpert() {
		this.setLogo("meltdowner/mine_expert.png");
		this.setName("md_mineexpert");
		setMaxLevel(15);
	}
	
	@Override
	float getConsume(int slv, int lv) {
		return 0.4f * (35 - slv * 0.8f - lv * 1.6f);
	}

	@Override
	int getHarvestLevel() {
		return 3;
	}

	@Override
	int getSpawnRate() {
		return 25;
	}

}

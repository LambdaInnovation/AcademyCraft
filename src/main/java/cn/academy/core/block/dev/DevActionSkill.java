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
package cn.academy.core.block.dev;

import cn.academy.api.data.AbilityData;
import cn.academy.core.client.ACLangs;

/**
 * @author WeathFolD
 *
 */
public class DevActionSkill implements IDevAction {
	
	int skillID;

	public DevActionSkill(int skillID) {
		this.skillID = skillID;
	}

	@Override
	public int getExpectedStims(AbilityData data) {
		return data.getSkill(skillID).getLearnCost(data);
	}

	@Override
	public void onActionFinished(AbilityData data) {
		data.upgrade(skillID);
	}

	@Override
	public String getActionInfo(AbilityData data) {
		return (data.isSkillLearned(skillID) ? ACLangs.upgradeSkill() : ACLangs.learnSkill()) + data.getSkill(skillID).getDisplayName();
	}

	@Override
	public double getSuccessfulRate(AbilityData data) {
		return Math.max(0.15, (0.8 - 0.15 * data.getLevelID())) * Math.pow(1 - dropRate(data.getLevelID()), data.getSkillLevel(skillID));
	}
	
	private double dropRate(int lvid) {
		return Math.min(0.06, 0.01 + lvid * 0.01);
	}

}

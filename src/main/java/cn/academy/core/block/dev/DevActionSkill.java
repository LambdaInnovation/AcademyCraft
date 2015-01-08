/**
 * 
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
		return 5;
	}

	@Override
	public void onActionFinished(AbilityData data) {
		if(!data.isSkillLearned(skillID)) {
			data.setSkillLevel(skillID, data.getSkillLevel(skillID) + 1);
		}
	}

	@Override
	public String getActionInfo(AbilityData data) {
		return ACLangs.learnSkill() + data.getSkill(skillID).getDisplayName();
	}

	@Override
	public double getSuccessfulRate(AbilityData data) {
		return Math.max(0.15, (0.8 - 0.15 * data.getLevelID())) * Math.pow(1 - dropRate(data.getLevelID()), data.getSkillLevel(skillID));
	}
	
	private double dropRate(int lvid) {
		return Math.min(0.06, 0.01 + lvid * 0.01);
	}

}

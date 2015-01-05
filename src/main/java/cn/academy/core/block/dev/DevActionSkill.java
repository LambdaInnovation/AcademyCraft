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
		// TODO Auto-generated method stub

	}

	@Override
	public String getActionInfo(AbilityData data) {
		return ACLangs.learnSkill() + data.getSkill(skillID).getDisplayName();
	}

}

/**
 * 
 */
package cn.academy.core.block.dev;

import cn.academy.api.ability.Abilities;
import cn.academy.api.data.AbilityData;

/**
 * @author WeathFolD
 *
 */
public class DevActionDevelop implements IDevAction {
	

	public DevActionDevelop(int placeholder) { }

	@Override
	public int getExpectedStims(AbilityData data) {
		return 9;
	}

	@Override
	public void onActionFinished(AbilityData data) {
		int cat = 1 + RNG.nextInt(Abilities.getCategories() - 1);
		data.setCategoryID(cat);
	}

	@Override
	public double getSuccessfulRate(AbilityData data) {
		return 0.67;
	}

	@Override
	public String getActionInfo(AbilityData data) {
		return "Developing new ability";
	}

}

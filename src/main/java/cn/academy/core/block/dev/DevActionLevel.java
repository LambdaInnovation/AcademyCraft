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
 * The level-up action. Handles level upgrading.
 * @author WeathFolD
 *
 */
public class DevActionLevel implements IDevAction {
	
	final int toLevel;

	public DevActionLevel(int level) {
		toLevel = level;
	}

	@Override
	public int getExpectedStims(AbilityData data) {
		return 2 * toLevel * toLevel + 3;
	}

	@Override
	public void onActionFinished(AbilityData data) {
		data.setLevelID(toLevel);
	}

	@Override
	public String getActionInfo(AbilityData data) {
		return ACLangs.upgradeTo() + data.getCategory().getLevel(toLevel).getDisplayName();
	}

	@Override
	public double getSuccessfulRate(AbilityData data) {
		return data.getLevel().getStimulationProb();
	}
	
}

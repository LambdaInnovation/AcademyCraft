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
package cn.academy.core.block.dev;

import cn.academy.api.ability.Abilities;
import cn.academy.api.data.AbilityData;
import cn.academy.core.client.ACLangs;

/**
 * @author WeathFolD
 *
 */
public class DevActionDevelop implements IDevAction {
	

	public DevActionDevelop(int placeholder) { }

	@Override
	public int getExpectedStims(AbilityData data) {
		return 6;
	}

	@Override
	public void onActionFinished(AbilityData data) {
		int cat = 1 + RNG.nextInt(Abilities.getCategoryCount() - 1);
		data.setCategoryID(cat);
	}

	@Override
	public double getSuccessfulRate(AbilityData data) {
		return 0.67;
	}

	@Override
	public String getActionInfo(AbilityData data) {
		return ACLangs.devNewAbility();
	}

}

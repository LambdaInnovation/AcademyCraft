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
package cn.academy.ability.api.ctrl.action;

import net.minecraft.world.World;
import cn.academy.ability.api.ctrl.SyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;

/**
 * A simple wrapper that setup the commonly used data and sandbox methods for Skill SyncActions.
 * @author WeAthFolD
 */
public class SkillSyncAction extends SyncAction {
	
	public AbilityData aData;
	public CPData cpData;
	public World world;

	protected SkillSyncAction(int interval) {
		super(interval);
	}
	
	@Override
	public void onStart() {
		aData = AbilityData.get(player);
		cpData = CPData.get(player);
		world = player.worldObj;
	}
	
}

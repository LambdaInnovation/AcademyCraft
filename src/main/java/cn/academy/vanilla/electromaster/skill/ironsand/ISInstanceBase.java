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
package cn.academy.vanilla.electromaster.skill.ironsand;

import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.vanilla.electromaster.skill.ironsand.IronSand.IronSandAction;

/**
 * @author WeAthFolD
 */
public abstract class ISInstanceBase extends SkillInstance {
	
	final String name;
	
	public ISInstanceBase(String _name) {
		name = _name;
	}
	
	@Override
	public final void onStart() {
		IronSandAction env = getEnv();
		if(env == null) {
			this.abortSkill();
			return;
		}
		
		if(!env.getCurrentType().equals(name)) {
			env.setCurrentType(name);
			abortSkill();
			return;
		}
		
		startSkill();
	}
	
	public void startSkill() {}
	
	protected IronSandAction getEnv() {
		return ActionManager.findAction(getPlayer(), IronSandAction.class);
	}
	
}

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
package cn.academy.ability.api.ctrl.instance;

import java.util.ArrayList;
import java.util.List;

import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.SyncAction;

/**
 * A wrapping for skills that just do something on click.
 * @author WeAthFolD
 */
public class SkillInstanceInstant extends SkillInstance {
	
	List<SyncAction> actions;

	public SkillInstanceInstant() {}
	
	@Override
	public final void onStart() {
		if(actions != null) {
			for(SyncAction act : actions)
				ActionManager.startAction(act);
		}
		
		execute();
		this.endSkill();
	}
	
	/**
	 * Called when this SkillInstance is executed. You can do additional stuff in player's client.
	 */
	public void execute() {}
	
	/**
	 * Automatically exeute this action on instance start.
	 * @param action
	 */
	public SkillInstanceInstant addExecution(SyncAction action) {
		if(actions == null) {
			actions = new ArrayList();
		}
		actions.add(action);
		return this;
	}

}

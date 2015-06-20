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

import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SyncAction;

/**
 * SyncAction that only does something on creation but needs additional validation in both sides.
 * @author WeAthFolD
 */
public abstract class SyncActionInstant extends SyncAction {

	public SyncActionInstant() {
		super(-1);
	}
	
	@Override
	public final void onStart() {
		if(!isRemote) {
			if(!validate())
				ActionManager.abortAction(this);
			else
				ActionManager.endAction(this);
		}
	}
	
	@Override
	public final void onEnd() {
		execute();
	}
	
	/**
	 * Check if this action is to be executed.
	 */
	public abstract boolean validate();
	
	/**
	 * Execute the action.
	 */
	public abstract void execute();

}

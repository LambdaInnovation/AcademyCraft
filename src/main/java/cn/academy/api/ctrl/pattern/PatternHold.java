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
package cn.academy.api.ctrl.pattern;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.ctrl.SkillEventType;
import cn.academy.api.ctrl.SkillState;
import cn.academy.api.ctrl.SkillStateManager;

public abstract class PatternHold extends Pattern {
	
	public static abstract class State extends SkillState {
		public State(EntityPlayer player) {
			super(player);
		}
		@Override
		public void onStart() {}
		@Override
		public boolean onFinish(boolean state) { return false; }
		public void onHold() {}
		@Override
		public final boolean isRemote() {
			return player.worldObj.isRemote;
		}
	}
	
	State state;
	int time;
	boolean holdCalled;

	public PatternHold(int time) {
		this.time = time;
	}
	
	@Override
	public boolean onRawEvent(EntityPlayer player, SkillEventType type,
			int rawTime, int time) {
		switch (type) {
		case RAW_DOWN:
			if (state != null) {
				state = null;
				return false;
			}
			holdCalled = false;
			state = createSkill(player);
			state.startSkill();
			if(player.worldObj.isRemote) {
				SkillStateManager.regPatternFor(state, this);
			}
			return false;
		case RAW_UP:
		case RAW_CANCEL:
			if (state == null) return false;
			state.finishSkill(type == SkillEventType.RAW_UP);
			state = null;
			return false;
		case RAW_TICK_DOWN:
		case RAW_ADJUST:
			if (!holdCalled && rawTime >= this.time) {
				holdCalled = true;
				state.onHold();
			}
		default:
			return false;
		}
	}

	public abstract State createSkill(EntityPlayer player);
}

package cn.academy.api.ctrl.pattern;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.ctrl.SkillEventType;
import cn.academy.api.ctrl.SkillState;

public abstract class PatternHold implements IPattern {
	
	public static abstract class State extends SkillState {
		public State(EntityPlayer player) {
			super(player);
		}
		public abstract void onStart();
		public abstract void onFinish();
		public abstract void onHold();
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
			return false;
		case RAW_UP:
		case RAW_CANCEL:
			if (state == null) return false;
			state.finishSkill();
			state = null;
			return false;
		case RAW_TICK_DOWN:
		case RAW_ADJUST:
			if (!holdCalled && rawTime >= this.time) {
				holdCalled = true;
				state.onHold(); //TODO: Crashed multiple times here because of NullPointerException
			}
		default:
			return false;
		}
	}

	public abstract State createSkill(EntityPlayer player);
}

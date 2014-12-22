package cn.academy.api.ctrl.pattern;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.ctrl.SkillEventType;
import cn.academy.api.ctrl.SkillState;

public abstract class PatternSimpleHold implements IPattern {
	
	private SkillState state;

	@Override
	public boolean onRawEvent(EntityPlayer player, SkillEventType type, int rawTime, int time) {
		switch (type) {
		case RAW_DOWN:
			if (state != null) {
				state = null;
				return false;
			}
			state = createSkill(player);
			state.startSkill();
			return false;
		case RAW_UP:
		case RAW_CANCEL:
			if (state == null) return false;
			state.finishSkill();
			state = null;
			return false;
		default:
			return false;
		}
	}

	public abstract SkillState createSkill(EntityPlayer player);
}

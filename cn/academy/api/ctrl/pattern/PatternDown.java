package cn.academy.api.ctrl.pattern;

import cn.academy.api.ctrl.SkillEventType;

public abstract class PatternDown implements IPattern {

	@Override
	public boolean onRawEvent(SkillEventType type, int rawTime, int time) {
		if (type == SkillEventType.RAW_DOWN) {
			return onKeyDown();
		}
		return false;
	}

	public abstract boolean onKeyDown();
	
}

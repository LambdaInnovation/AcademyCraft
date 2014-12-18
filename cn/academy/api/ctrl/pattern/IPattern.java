package cn.academy.api.ctrl.pattern;

import cn.academy.api.ctrl.SkillEventType;

public interface IPattern {
	
	/**
	 * On receiving a raw event from EventHandler.
	 * @param type The type of the raw event.
	 * @param rawTime If on server, sometimes the client time received. Use this time to determine pattern.
	 * @param time The time of RawEventHandler of this side. Use this time to trigger skill event.
	 * @return Return true to indicate that the skill needs to reset patterns. 
	 */
	boolean onRawEvent(SkillEventType type, int rawTime, int time);
	
}

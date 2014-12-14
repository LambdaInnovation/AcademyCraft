package cn.academy.api.ctrl;

/**
 * Event Type ID sent to a skill class.
 * @author acaly
 *
 */
public enum SkillEventType {
	
	/**
	 * When a key is pressed. 
	 * Raw Event. Don't use it in skill class directly.
	 */
	KEY_DOWN,
	
	/**
	 * When a key is released.
	 * Raw Event. Don't use it in skill class directly.
	 */
	KEY_UP,
	
	/**
	 * Start of a skill. 
	 * Usually right after KEY_DOWN.
	 */
	SKILL_START,
	
	/**
	 * End of a skill.
	 * Usually after KEY_UP, but can also have a delay.
	 */
	SKILL_END,
	
	/**
	 * When the player keeps pressing a key for some time (time provided by skill).
	 */
	KEY_TIMER_TRIGGERED,
	
}

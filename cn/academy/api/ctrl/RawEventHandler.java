package cn.academy.api.ctrl;

import java.util.ArrayList;
import java.util.List;

import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.pattern.IPattern;

/**
 * This class handles raw event and send the proper event to skill.
 * It has its own timer based on tick event called by EventHandler.
 * There's no difference in its behavior of client side and server side instances. 
 * @author acaly
 *
 */
public class RawEventHandler {
	
	public RawEventHandler(SkillBase skill) {
		skill.initPattern(this);
	}
	
	/*
	 * Global constants used by EventHandlers
	 */

	public static final int KA_INTERVAL = 20,
						KA_DELAY = 10,
						DBL_DELAY = 10;
	
	/**
	 * Receive raw event. Judge with time given time.
	 * Trigger skill event with its own time.
	 * @param type
	 * @param time On server, it's time on client (sent in Message). On client, it's time get by getTime.
	 */
	public void onEvent(SkillEventType type, int time) {
		/* 
		 * Event type handled:
		 * RAW_DOWN, RAW_UP, RAW_TICK_DOWN, RAW_TICK_UP, RAW_CANCEL, RAW_ADJUST, RAW_CLICK, RAW_DBLCLK.
		 */
		for (IPattern pattern : patterns) {
			pattern.onRawEvent(type, time, this.getTime());
		}
		switch (type) {
		case RAW_TICK_DOWN:
		case RAW_TICK_UP:
			++this.time;
			break;
		case RAW_DOWN:
			this.time = 0;//reset timer
			break;
		default:
			break;
		}
	}
	
	int time;
	/**
	 * Get current skill time on this side.
	 * @return
	 */
	int getTime() {
		return time;
	}
	
	private List<IPattern> patterns = new ArrayList();
	
	public void clear() {
		patterns.clear();
	}
	
	public void addPattern(IPattern pattern) {
		patterns.add(pattern);
	}
}

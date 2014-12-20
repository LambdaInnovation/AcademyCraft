package cn.academy.api.ctrl;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.pattern.IPattern;

/**
 * This class handles raw event and send the proper event to skill (through patterns).
 * It has its own timer based on tick event called by EventHandler.
 * There's no difference in its behavior of client side and server side instances. 
 * @author acaly
 *
 */
public class RawEventHandler {
	
	private SkillBase skill;
	private EntityPlayer player;
	
	RawEventHandler(EntityPlayer player, SkillBase skill) {
		skill.initPattern(this);
		this.skill = skill;
		this.player = player;
	}
	
	/**
	 * Network constants used by EventHandlers
	 */
	static final int KA_INTERVAL = 20,
						KA_DELAY = 10,
						DBL_DELAY = 10;
	
	/**
	 * Receive raw event. Judge with time given.
	 * Trigger skill event with its own time.
	 * @param type The event type.
	 * @param time On server, it's time on client (sent in Message). On client, it's time get by getTime.
	 */
	void onEvent(SkillEventType type, int time) {
		/* 
		 * Event type handled (same between server and client):
		 * RAW_DOWN, RAW_UP, RAW_TICK_DOWN, RAW_TICK_UP, RAW_CANCEL, RAW_ADJUST, RAW_CLICK, RAW_DBLCLK.
		 */
		boolean resetFlag = false;
		for (IPattern pattern : patterns) {
			resetFlag = pattern.onRawEvent(player, type, time, this.getTime()) || resetFlag;
		}
		if (resetFlag) {
			patterns.clear();
			skill.initPattern(this);
		}
		
		switch (type) {
		case RAW_TICK_DOWN:
		case RAW_TICK_UP:
			//Increase timer.
			++this.time;
			break;
		case RAW_DOWN:
			//Reset timer.
			//Note that at this time RAW_DOWN has been invoked,
			//So the patterns can get the time in RAW_DOWN event.
			this.time = 1;
			break;
		default:
			break;
		}
	}
	
	/**
	 * Internal timer.
	 */
	private int time;
	
	/**
	 * Get current skill time on this side.
	 * @return The time.
	 */
	int getTime() {
		return time;
	}
	
	/**
	 * The current patterns of this skill. 
	 */
	private List<IPattern> patterns = new ArrayList();
	
	/**
	 * Clear all patterns.
	 * Don't directly use this function.
	 */
	public void clear() {
		patterns.clear();
	}
	
	/**
	 * Add a new pattern. Used by skills in initPattern.
	 * @param pattern The pattern to be added.
	 */
	public void addPattern(IPattern pattern) {
		patterns.add(pattern);
	}
}

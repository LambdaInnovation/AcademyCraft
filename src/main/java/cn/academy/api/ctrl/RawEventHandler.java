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
package cn.academy.api.ctrl;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.pattern.Pattern;
import cn.academy.api.ctrl.pattern.PatternDown;

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
	
	
	public RawEventHandler(EntityPlayer player, SkillBase skill) {
		skill.initPattern(this);
		this.skill = skill;
		this.player = player;
	}
	
	public SkillBase getSkill() {
		return skill;
	}
	
	/**
	 * Network constants used by EventHandlers
	 */
	public static final int KA_INTERVAL = 50,
						KA_DELAY = 50,
						DBL_DELAY = 10;
	
	/**
	 * Receive raw event. Judge with time given.
	 * Trigger skill event with its own time.
	 * @param type The event type.
	 * @param time On server, it's time on client (sent in Message). On client, it's time get by getTime.
	 * @return arbitary on server, true on client if needed to send events to server.
	 */
	public boolean onEvent(SkillEventType type, int time, boolean client) {
		
		//CD hack, needs modifying to support more than 1 patterns.
		if(client) {
			if(type == SkillEventType.RAW_DOWN) {
				//If any pattern doesn't receive event, don't send any event at all.
				for(Pattern p : patterns) {
					if(!p.receivesEvent()) {
						return false;
					}
				}
			}
		}
		if(type == SkillEventType.RAW_DOWN)
			System.out.println("exec " + skill);
		
		/* 
		 * Event type handled (same between server and client):
		 * RAW_DOWN, RAW_UP, RAW_TICK_DOWN, RAW_TICK_UP, RAW_CANCEL, RAW_ADJUST, RAW_CLICK, RAW_DBLCLK.
		 */
		boolean resetFlag = false;
		for (Pattern pattern : patterns) {
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
		return true;
	}
	
	/**
	 * Internal timer.
	 */
	private int time;
	
	/**
	 * Get current skill time on this side.
	 * @return The time.
	 */
	public int getTime() {
		return time;
	}
	
	/**
	 * The current patterns of this skill. 
	 */
	private List<Pattern> patterns = new ArrayList();
	
	/**
	 * Clear all patterns.
	 * Don't directly use this function.
	 */
	public void clear() {
		patterns.clear();
	}
	
	public Pattern getPattern(int i) {
		return patterns.get(i);
	}
	
	/**
	 * Add a new pattern. Used by skills in initPattern.
	 * @param pattern The pattern to be added.
	 */
	public void addPattern(Pattern pattern) {
		patterns.add(pattern);
		pattern.reh = this;
	}
}

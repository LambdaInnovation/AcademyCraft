/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.api.ctrl.pattern;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.ctrl.SkillEventType;
import cn.academy.api.ctrl.SkillState;

/**
 * The pattern designed to be used in simple skill.
 * Options:
 * - prepareTick: The tick before the skill is actually activated (can be 0).
 * - rigTick:     The tick after the skill starts and before the player can move 
 *                (behavior is controlled by skill).
 * @author acaly
 *
 */
public abstract class PatternSimpleSkill extends Pattern {
	
	private int prep;
	private boolean holdCalled;
	
	/**
	 * The state created but not started.
	 */
	private SkillState currentState;

	public PatternSimpleSkill(int prepareTick) {
		this.prep = prepareTick;
	}
	
	@Override
	public boolean onRawEvent(EntityPlayer player, SkillEventType type, int rawTime, int time) {
		switch (type) {
		case RAW_DOWN:
			if (currentState != null) {
				currentState = null;
				return false;
			}
			currentState = onPatternStart(player);
			if (currentState == null) {
				return false;
			}
			holdCalled = false;
			return false;
		case RAW_UP:
		case RAW_CANCEL:
			currentState = null;
			return false;
		case RAW_TICK_DOWN:
		case RAW_ADJUST:
			if (currentState != null && !holdCalled && rawTime >= this.prep) {
				holdCalled = true;
				currentState.startSkill();
			}
			return false;
		default:
			return false;
		}
	}
	
	public abstract SkillState onPatternStart(EntityPlayer player);
}

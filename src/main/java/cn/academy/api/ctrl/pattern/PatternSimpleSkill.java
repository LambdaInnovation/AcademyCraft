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
public abstract class PatternSimpleSkill implements IPattern {
	
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

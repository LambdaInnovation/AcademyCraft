package cn.academy.api.ctrl.pattern;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.ctrl.SkillEventType;

public abstract class PatternDown implements IPattern {

	@Override
	public boolean onRawEvent(EntityPlayer player, SkillEventType type, int rawTime, int time) {
		if (type == SkillEventType.RAW_DOWN) {
			return onKeyDown(player);
		}
		return false;
	}

	public abstract boolean onKeyDown(EntityPlayer player);
	
}

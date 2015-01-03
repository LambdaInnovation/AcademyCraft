package cn.academy.api.ctrl.pattern.internal;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.ctrl.SkillEventType;
import cn.academy.api.ctrl.pattern.IPattern;

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

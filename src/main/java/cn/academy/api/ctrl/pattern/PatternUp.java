package cn.academy.api.ctrl.pattern;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.ctrl.SkillEventType;

public abstract class PatternUp implements IPattern {

	@Override
	public boolean onRawEvent(EntityPlayer player, SkillEventType type, int rawTime, int time) {
		if (type == SkillEventType.RAW_UP) {
			return onKeyUp(player);
		}
		return false;
	}

	public abstract boolean onKeyUp(EntityPlayer player);

}

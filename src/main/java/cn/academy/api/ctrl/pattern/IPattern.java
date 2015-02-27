/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
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

public interface IPattern {
	
	/**
	 * On receiving a raw event from EventHandler.
	 * @param type The type of the raw event.
	 * @param rawTime If on server, sometimes the client time received. Use this time to determine pattern.
	 * @param time The time of RawEventHandler of this side. Use this time to trigger skill event.
	 * @return Return true to indicate that the skill needs to reset patterns. 
	 */
	boolean onRawEvent(EntityPlayer player, SkillEventType type, int rawTime, int time);
	
}

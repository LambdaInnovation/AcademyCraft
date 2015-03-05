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
package cn.academy.api.event;

import cn.academy.api.ability.SkillBase;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Fired when player successfully performs a skill and CD was updated, client only.
 * @author WeathFolD
 */
@SideOnly(Side.CLIENT)
public class UpdateCDEvent extends Event {
	
	public final SkillBase skill;
	public final int cd;
	
	public UpdateCDEvent(SkillBase _skill, int _cd) {
		skill = _skill;
		cd = _cd;
	}
}

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
package cn.academy.vanilla.meltdowner.skill;

import cn.academy.ability.api.Skill;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

/**
 * Just a placeholder. implementation is in MDDamageHelper.
 * @author WeAthFolD
 */
public class RadiationIntensify extends Skill {

	public RadiationIntensify() {
		super("rad_intensify", 1);
		this.canControl = false;
	}

}

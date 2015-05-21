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
package cn.academy.ability.impl;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.CategoryManager;
import cn.academy.ability.impl.electromaster.skill.SkillArcGen;
import cn.academy.ability.impl.electromaster.skill.SkillMagAttract;
import cn.academy.ability.impl.electromaster.skill.SkillMineDetect;
import cn.academy.ability.impl.electromaster.skill.SkillRailgun;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegSubmoduleInit;

/**
 * @author WeAthFolD
 *
 */
@Registrant
@RegSubmoduleInit
public class VanillaAbilities {
	
	public static void init() {
		//Electro master
		{
			Category cat = new Category("electro_master");
			
			cat.addSkill(new SkillArcGen());
			cat.addSkill(new SkillMagAttract());
			cat.addSkill(new SkillMineDetect());
			cat.addSkill(new SkillRailgun());
			
			CategoryManager.INSTANCE.register(cat);
		}
	}
	
	
}

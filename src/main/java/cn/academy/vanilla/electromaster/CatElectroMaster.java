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
package cn.academy.vanilla.electromaster;

import cn.academy.ability.api.Category;
import cn.academy.vanilla.electromaster.skill.SkillArcGen;
import cn.academy.vanilla.electromaster.skill.SkillMagAttract;
import cn.academy.vanilla.electromaster.skill.SkillMineDetect;
import cn.academy.vanilla.electromaster.skill.SkillRailgun;

/**
 * @author WeAthFolD
 *
 */
public class CatElectroMaster extends Category {
	
	public SkillArcGen arcGen;
	public SkillMagAttract magAttract;
	public SkillMineDetect mineDetect;
	public SkillRailgun railgun;

	public CatElectroMaster() {
		super("electro_master");
		
		defineTypes("default");
		
		addSkill("default", arcGen = new SkillArcGen());
		addSkill("default", magAttract = new SkillMagAttract());
		addSkill("default", mineDetect = new SkillMineDetect());
		addSkill("default", railgun = new SkillRailgun());
		
		magAttract.setParent(arcGen);
		mineDetect.setParent(magAttract);
		railgun.setParent(magAttract);
	}

}

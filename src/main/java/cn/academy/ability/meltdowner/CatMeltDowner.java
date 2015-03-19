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
package cn.academy.ability.meltdowner;

import cn.academy.ability.meltdowner.skill.SkillBomb;
import cn.academy.ability.meltdowner.skill.SkillLightShield;
import cn.academy.ability.meltdowner.skill.SkillMeltDowner;
import cn.academy.ability.meltdowner.skill.SkillMiningAcc;
import cn.academy.ability.meltdowner.skill.SkillMiningBasic;
import cn.academy.ability.meltdowner.skill.SkillMiningExpert;
import cn.academy.ability.meltdowner.skill.SkillMiningLuck;
import cn.academy.ability.meltdowner.skill.SkillScatterBomb;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;
import cn.academy.api.ability.SkillBase;
import cn.academy.core.register.AbilityRegistration.RegAbility;
import cn.annoreg.core.RegistrationClass;

/**
 * @author acaly, WeAthFolD
 *
 */
@RegistrationClass
@RegAbility
public class CatMeltDowner extends Category {
	
	public static CatMeltDowner INSTANCE;
	
	public static SkillBomb bomb;
	public static SkillMiningBasic mineBasic;
	public static SkillScatterBomb scatterBomb;
	public static SkillLightShield shield;
	public static SkillMeltDowner meltDowner;
	public static SkillMiningExpert mineExpert;
	public static SkillMiningLuck mineLuck;
	public static SkillMiningAcc mineAcc;

	@Override
	protected void register() {
		INSTANCE = this;
		
		this.addLevel(new Level(this, 1000.0f, 1700.0f, 0.5f, 1.0f, .9));
		this.addLevel(new Level(this, 2000.0f, 3000.0f, 1.5f, 1.8f, .8));
		this.addLevel(new Level(this, 3500.0f, 5500.0f, 2.2f, 2.6f, .7));
		this.addLevel(new Level(this, 6000.0f, 9000.0f, 3.0f, 3.5f, .4));
		this.addLevel(new Level(this, 10000.0f, 40000.0f, 4.0f, 5.0f, .2));
		
		this.addSkill(new SkillBase(), 0);
		this.addSkill(bomb = new SkillBomb(), 0);
		this.addSkill(mineBasic = new SkillMiningBasic(), 0);
		this.addSkill(scatterBomb = new SkillScatterBomb(), 1);
		this.addSkill(shield = new SkillLightShield(), 1);
		this.addSkill(meltDowner = new SkillMeltDowner(), 2);
		this.addSkill(mineExpert = new SkillMiningExpert(), 2);
		this.addSkill(mineLuck = new SkillMiningLuck(), 3);
		this.addSkill(mineAcc = new SkillMiningAcc(), 4);
		
		this.setLogo("meltdowner/main.png");
		this.setColorStyle(72, 214, 79);
	}
	
	@Override
	public String getInternalName() {
		return "meltdowner";
	}
}

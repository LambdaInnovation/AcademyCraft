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
package cn.academy.ability.electro;

import cn.academy.ability.electro.skill.SkillBuff;
import cn.academy.ability.electro.skill.SkillItemCharge;
import cn.academy.ability.electro.skill.SkillLightningFlash;
import cn.academy.ability.electro.skill.SkillMagMove;
import cn.academy.ability.electro.skill.SkillMineDetect;
import cn.academy.ability.electro.skill.SkillRailgun;
import cn.academy.ability.electro.skill.SkillStrongArc;
import cn.academy.ability.electro.skill.SkillWeakArc;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;
import cn.academy.api.ability.SkillBase;
import cn.academy.core.register.AbilityRegistration.RegAbility;
import cn.annoreg.core.RegistrationClass;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegAbility
public class CatElectro extends Category {
	
	public static CatElectro INSTANCE;
	
	public static SkillWeakArc weakArc;
	public static SkillItemCharge itemCharge;
	public static SkillStrongArc strongArc;
	public static SkillMagMove magMovement;
	public static SkillRailgun railgun;
	public static SkillMineDetect mineDetect;
	public static SkillLightningFlash lightningFlash;
	public static SkillBuff buff;

	@Override
	protected void register() {
		INSTANCE = this; //TODO: Add this support in AnnoReg?
		
		this.addLevel(new Level(this, 1000.0f, 1700.0f, 0.5f, 1.0f, .8));
		this.addLevel(new Level(this, 2000.0f, 3000.0f, 1.5f, 1.8f, .7));
		this.addLevel(new Level(this, 3500.0f, 5500.0f, 2.2f, 2.6f, .6));
		this.addLevel(new Level(this, 6000.0f, 9000.0f, 3.0f, 3.5f, .5));
		this.addLevel(new Level(this, 10000.0f, 30000.0f, 4.0f, 5.0f, .3));
		
		this.addSkill(new SkillBase(), 0);
		this.addSkill(weakArc = new SkillWeakArc(), 0);
		this.addSkill(itemCharge = new SkillItemCharge(), 0);
		this.addSkill(magMovement = new SkillMagMove(), 1);
		this.addSkill(buff = new SkillBuff(), 2);
		this.addSkill(strongArc = new SkillStrongArc(), 2);
		this.addSkill(mineDetect = new SkillMineDetect(), 2);
		this.addSkill(railgun = new SkillRailgun(), 3);
		this.addSkill(lightningFlash = new SkillLightningFlash(), 4);
		
		this.setColorStyle(26, 143, 218);
		this.setLogo("electro/main.png");
	}
	
	@Override
	public String getInternalName() {
		return "elecmaster";
	}

}

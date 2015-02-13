package cn.academy.ability.meltdowner;

import cn.academy.ability.meltdowner.skill.SkillBomb;
import cn.academy.ability.meltdowner.skill.SkillLightShield;
import cn.academy.ability.meltdowner.skill.SkillMeltDowner;
import cn.academy.ability.meltdowner.skill.SkillMiningAcc;
import cn.academy.ability.meltdowner.skill.SkillMiningBasic;
import cn.academy.ability.meltdowner.skill.SkillMiningExpert;
import cn.academy.ability.meltdowner.skill.SkillMiningLuck;
import cn.academy.ability.meltdowner.skill.SkillRayBarrage;
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
	
	CatMeltDowner INSTANCE;

	@Override
	protected void register() {
		INSTANCE = this;
		
		this.addLevel(new Level(this, 800.0f, 1800.0f, 0.5f, 1.0f, .9));
		this.addLevel(new Level(this, 2000.0f, 3000.0f, 1.5f, 1.8f, .8));
		this.addLevel(new Level(this, 3500.0f, 5500.0f, 2.2f, 2.6f, .7));
		this.addLevel(new Level(this, 6000.0f, 9000.0f, 3.0f, 3.5f, .4));
		this.addLevel(new Level(this, 10000.0f, Float.MAX_VALUE, 4.0f, 5.0f, .2));
		
		this.addSkill(new SkillBase(), 0);
		//TODO: Correct levels
		this.addSkill(new SkillBomb(), 0);
		this.addSkill(new SkillMiningBasic(), 0);
		this.addSkill(new SkillScatterBomb(), 0);
		this.addSkill(new SkillLightShield(), 0);
		this.addSkill(new SkillMeltDowner(), 0);
		this.addSkill(new SkillMiningExpert(), 0);
		this.addSkill(new SkillRayBarrage(), 0);
		this.addSkill(new SkillMiningLuck(), 0);
		this.addSkill(new SkillMiningAcc(), 0);
		
		this.setLogo("meltdowner/main.png");
		this.setColorStyle(72, 214, 79);
	}
	
	@Override
	public String getInternalName() {
		return "meltdowner";
	}
}

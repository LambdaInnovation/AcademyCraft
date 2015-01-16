package cn.academy.ability.meltdowner;

import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cn.academy.ability.meltdowner.skill.SkillElecDart;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;
import cn.academy.api.ability.SkillBase;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;
import cn.academy.core.register.AbilityRegistration.RegAbility;
import cn.annoreg.core.RegistrationClass;

/**
 * @author acaly
 *
 */
@RegistrationClass
@RegAbility
public class CatMeltDowner extends Category {

	@Override
	protected void register() {
		this.addLevel(new Level(this, 400.0f, 800.0f, 0.5f, 1.0f, .9));
		this.addLevel(new Level(this, 800.0f, 2000.0f, 1.5f, 1.8f, .8));
		this.addLevel(new Level(this, 2000.0f, 3500.0f, 2.2f, 2.6f, .7));
		this.addLevel(new Level(this, 3500.0f, 6000.0f, 3.0f, 3.5f, .4));
		this.addLevel(new Level(this, 6000.0f, 10000.0f, 4.0f, 5.0f, .2));
		
		this.addSkill(new SkillBase(), 0);
		this.addSkill(new SkillElecDart(), 0);
	}

	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.MD_LOGO;
	}
	
	@Override
	public String getInternalName() {
		return "meltdowner";
	}
}

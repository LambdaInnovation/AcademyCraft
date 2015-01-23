/**
 * 
 */
package cn.academy.ability.electro;

import net.minecraft.util.ResourceLocation;
import cn.academy.ability.electro.skill.SkillArcGen;
import cn.academy.ability.electro.skill.SkillItemCharge;
import cn.academy.ability.electro.skill.SkillMagneticMovement;
import cn.academy.ability.electro.skill.SkillMineDetect;
import cn.academy.ability.electro.skill.SkillRailgun;
import cn.academy.ability.electro.skill.SkillStrongArc;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;
import cn.academy.api.ability.SkillBase;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.core.register.AbilityRegistration.RegAbility;
import cn.annoreg.core.RegistrationClass;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegAbility
public class CatElectro extends Category {
	
	CatElectro INSTANCE;

	@Override
	protected void register() {
		INSTANCE = this;
		this.addLevel(new Level(this, 800.0f, 1800.0f, 0.5f, 1.0f, .8));
		this.addLevel(new Level(this, 2000.0f, 3000.0f, 1.5f, 1.8f, .7));
		this.addLevel(new Level(this, 3500.0f, 5500.0f, 2.2f, 2.6f, .6));
		this.addLevel(new Level(this, 6000.0f, 9000.0f, 3.0f, 3.5f, .5));
		this.addLevel(new Level(this, 10000.0f, Float.MAX_VALUE, 4.0f, 5.0f, .3));
		
		this.addSkill(new SkillBase(), 0);
		this.addSkill(new SkillArcGen(), 0);
		this.addSkill(new SkillItemCharge(), 0);
		this.addSkill(new SkillStrongArc(), 0);
		this.addSkill(new SkillMagneticMovement(), 0);
		this.addSkill(new SkillRailgun(), 0);
		this.addSkill(new SkillMineDetect(), 0);
		
		this.setColorStyle(26, 143, 218);
	}
	
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.ELEC_LOGO;
	}
	
	@Override
	public String getInternalName() {
		return "elecmaster";
	}

}

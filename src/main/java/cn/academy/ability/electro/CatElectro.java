/**
 * 
 */
package cn.academy.ability.electro;

import net.minecraft.util.ResourceLocation;
import cn.academy.ability.electro.skill.SkillElecArc;
import cn.academy.ability.electro.skill.SkillItemCharge;
import cn.academy.ability.electro.skill.SkillMagneticMovement;
import cn.academy.ability.electro.skill.SkillRailgun;
import cn.academy.ability.electro.skill.SkillStrongArc;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ability.AbilityRegistration.RegAbility;
import cn.academy.core.proxy.ACClientProps;
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

	@Override
	protected void register() {
		this.addLevel(new Level(this, 400.0f, 800.0f, 0.5f, 1.0f));
		this.addLevel(new Level(this, 800.0f, 2000.0f, 1.5f, 1.8f));
		this.addLevel(new Level(this, 2000.0f, 3500.0f, 2.2f, 2.6f));
		this.addLevel(new Level(this, 3500.0f, 6000.0f, 3.0f, 3.5f));
		this.addLevel(new Level(this, 6000.0f, 10000.0f, 4.0f, 5.0f));
		
		this.addSkill(new SkillBase(), 0);
		this.addSkill(new SkillElecArc(), 0);
		this.addSkill(new SkillItemCharge(), 0);
		this.addSkill(new SkillStrongArc(), 0);
		this.addSkill(new SkillMagneticMovement(), 0);
		this.addSkill(new SkillRailgun(), 0);
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

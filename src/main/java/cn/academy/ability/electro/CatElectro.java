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
import cn.academy.core.proxy.ACClientProps;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
public class CatElectro extends Category {

	public CatElectro() {
		super();
		init();
	}
	
	private void init() {
		Level lv0 = new Level(this, 0, 0, 0, 0, 0);
		lv0.addCanLearnSkill(this.addSkill(new SkillBase()));
		lv0.addCanLearnSkill(this.addSkill(new SkillElecArc()));
		lv0.addCanLearnSkill(this.addSkill(new SkillItemCharge()));
		lv0.addCanLearnSkill(this.addSkill(new SkillStrongArc()));
		lv0.addCanLearnSkill(this.addSkill(new SkillMagneticMovement()));
		lv0.addCanLearnSkill(this.addSkill(new SkillRailgun()));
		this.addLevel(lv0);
		
		this.addLevel(new Level(this, 1, 400, 800, 0.5f, 1.0f));
		this.addLevel(new Level(this, 2, 800, 2000, 1.5f, 1.8f));
		this.addLevel(new Level(this, 3, 2000, 3500, 2.2f, 2.6f));
		this.addLevel(new Level(this, 4, 3500, 6000, 3.0f, 3.5f));
		this.addLevel(new Level(this, 5, 6000, 10000, 4.0f, 5.0f));
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

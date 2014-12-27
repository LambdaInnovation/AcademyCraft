/**
 * 
 */
package cn.academy.ability.electro;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.ResourceLocation;
import cn.academy.ability.electro.skill.SkillElecArc;
import cn.academy.ability.electro.skill.SkillItemCharge;
import cn.academy.ability.electro.skill.SkillMagneticMovement;
import cn.academy.ability.electro.skill.SkillRailgun;
import cn.academy.ability.electro.skill.SkillStrongArc;
import cn.academy.api.ability.Abilities;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ability.levels.LevelBase0;
import cn.academy.api.ability.levels.LevelBase1;
import cn.academy.api.ability.levels.LevelBase2;
import cn.academy.api.ability.levels.LevelBase3;
import cn.academy.api.ability.levels.LevelBase4;
import cn.academy.api.ability.levels.LevelBase5;
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
		Level lv0 = new LevelBase0(this);
		lv0.addCanLearnSkill(this.addSkill(new SkillBase()));
		lv0.addCanLearnSkill(this.addSkill(new SkillElecArc()));
		lv0.addCanLearnSkill(this.addSkill(new SkillItemCharge()));
		lv0.addCanLearnSkill(this.addSkill(new SkillStrongArc()));
		lv0.addCanLearnSkill(this.addSkill(new SkillMagneticMovement()));
		lv0.addCanLearnSkill(this.addSkill(new SkillRailgun()));
		this.addLevel(lv0);
		
		this.addLevel(new LevelBase1(this));
		this.addLevel(new LevelBase2(this));
		this.addLevel(new LevelBase3(this));
		this.addLevel(new LevelBase4(this));
		this.addLevel(new LevelBase5(this));
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

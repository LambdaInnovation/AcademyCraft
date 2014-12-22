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
import cn.academy.api.ability.SkillBase;
import cn.academy.core.proxy.ACClientProps;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
public class CatElectro extends Category {
	
	private static List<SkillBase> skills = new ArrayList<SkillBase>();
	static {
		skills.add(new SkillElecArc());
		skills.add(new SkillItemCharge());
		skills.add(new SkillStrongArc());
		skills.add(new SkillMagneticMovement());
		skills.add(new SkillRailgun());
		Abilities.registerSkill(skills);
	}

	public CatElectro() {
		super(skills);
	}
	
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.ELEC_LOGO;
	}

}

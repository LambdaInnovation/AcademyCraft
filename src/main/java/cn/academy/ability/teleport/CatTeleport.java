package cn.academy.ability.teleport;

import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cn.academy.ability.teleport.skill.SkillBrainTrainingCourse;
import cn.academy.ability.teleport.skill.SkillBrainTrainingCourse2;
import cn.academy.ability.teleport.skill.SkillHighSpeedTele;
import cn.academy.ability.teleport.skill.SkillLocatingTele;
import cn.academy.ability.teleport.skill.SkillMarkTele;
import cn.academy.ability.teleport.skill.SkillPenetrateTele;
import cn.academy.ability.teleport.skill.SkillSpiritTrainingCourse;
import cn.academy.ability.teleport.skill.SkillStringTheory;
import cn.academy.ability.teleport.skill.SkillThreatingTele;
import cn.academy.ability.teleport.skill.SkillViscusStripping;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;
import cn.academy.api.ability.SkillBase;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.core.register.AbilityRegistration.RegAbility;
import cn.annoreg.core.RegistrationClass;

/**
 * 
 * @author mkpoli_
 *
 */
@RegistrationClass
@RegAbility
public class CatTeleport extends Category {
	public static CatTeleport INSTANCE;

	public static SkillThreatingTele skillThreateningTele;
	public static SkillStringTheory skillStringTheory;
	public static SkillPenetrateTele skillPenetrateTele;
	public static SkillMarkTele skillMarkedTele;
	public static SkillViscusStripping skillViscusStripping;
	public static SkillLocatingTele skillLocatingTele;
	// 待定： 弦理论进修 ExStringTheory
	public static SkillBrainTrainingCourse skillBrainTrainingCourse;
	public static SkillHighSpeedTele skillHighSpeedTele;
	public static SkillBrainTrainingCourse2 skillBrainTrainingCourse2;
	public static SkillSpiritTrainingCourse skillSpiritTrainingCourse;

	// public final static ResourceLocation TELE_LOGO = new
	// ResourceLocation("academy:textures/abilities/teleport/main.png");
	public final static ResourceLocation TELE_LOGO = ACClientProps.TEX_QUESTION_MARK;

	@Override
	protected void register() {
		INSTANCE = this;
		// Minecraft.getMinecraft().thePlayer.e

		// TODO : Edit data
		this.addLevel(new Level(this, 800.0f, 1800.0f, 0.5f, 1.0f, .9));
		this.addLevel(new Level(this, 2000.0f, 3000.0f, 1.5f, 1.8f, .8));
		this.addLevel(new Level(this, 3500.0f, 5500.0f, 2.2f, 2.6f, .7));
		this.addLevel(new Level(this, 6000.0f, 9000.0f, 3.0f, 3.5f, .4));
		this.addLevel(new Level(this, 10000.0f, Float.MAX_VALUE, 4.0f, 5.0f, .2));

		this.addSkill(new SkillBase(), 0);
		this.addSkill(skillThreateningTele = new SkillThreatingTele(), 0);
		this.addSkill(skillStringTheory = new SkillStringTheory(), 0);
		this.addSkill(skillPenetrateTele = new SkillPenetrateTele(), 0);
		this.addSkill(skillMarkedTele = new SkillMarkTele(), 0);
		this.addSkill(skillViscusStripping = new SkillViscusStripping(), 0);
		this.addSkill(skillLocatingTele = new SkillLocatingTele(), 0);
		this.addSkill(
				skillBrainTrainingCourse = new SkillBrainTrainingCourse(), 0);
		this.addSkill(skillHighSpeedTele = new SkillHighSpeedTele(), 0);
		this.addSkill(
				skillBrainTrainingCourse2 = new SkillBrainTrainingCourse2(), 0);
		this.addSkill(
				skillSpiritTrainingCourse = new SkillSpiritTrainingCourse(), 0);
		// this.addSkill(skillDangerTele = new SkillDangerTele(), 0);
		// this.addSkill(skill, 0);
	}

	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return TELE_LOGO;
	}

	@Override
	public String getInternalName() {
		return "teleport";
	}
}

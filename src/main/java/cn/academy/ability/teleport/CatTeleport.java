package cn.academy.ability.teleport;

import cn.academy.ability.generic.skill.SkillBrainTrainingCourse;
import cn.academy.ability.generic.skill.SkillBrainTrainingCourse2;
import cn.academy.ability.generic.skill.SkillSpiritTrainingCourse;
import cn.academy.ability.teleport.skill.SkillHighSpeedTele;
import cn.academy.ability.teleport.skill.SkillLocatingTele;
import cn.academy.ability.teleport.skill.SkillMarkTele;
import cn.academy.ability.teleport.skill.SkillPenetrateTele;
import cn.academy.ability.teleport.skill.SkillStringTheory;
import cn.academy.ability.teleport.skill.SkillThreateningTele;
import cn.academy.ability.teleport.skill.SkillViscusStripping;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.data.AbilityData;
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

	public static SkillThreateningTele skillThreateningTele;
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

	@Override
	protected void register() {
		INSTANCE = this;
		// Minecraft.getMinecraft().thePlayer.e

		// TODO : Edit data
		// TODO: Buff skills not added in β. Waiting for implementation.
		this.addLevel(new Level(this, 800.0f, 1800.0f, 0.5f, 1.0f, .9));
		this.addLevel(new Level(this, 2000.0f, 3000.0f, 1.5f, 1.8f, .8));
		this.addLevel(new Level(this, 3500.0f, 5500.0f, 2.2f, 2.6f, .7));
		this.addLevel(new Level(this, 6000.0f, 9000.0f, 3.0f, 3.5f, .4));
		this.addLevel(new Level(this, 10000.0f, Float.MAX_VALUE, 4.0f, 5.0f, .2));

		this.addSkill(new SkillBase(), 0);
		this.addSkill(skillThreateningTele = new SkillThreateningTele(), 0);
		//this.addSkill(skillStringTheory = new SkillStringTheory(), 0);
		this.addSkill(skillMarkedTele = new SkillMarkTele(), 1);
		this.addSkill(skillPenetrateTele = new SkillPenetrateTele(), 1);
		this.addSkill(skillViscusStripping = new SkillViscusStripping(), 2);
		this.addSkill(skillLocatingTele = new SkillLocatingTele(), 3);
		//this.addSkill(skillBrainTrainingCourse = new SkillBrainTrainingCourse(), 0);
		//this.addSkill(skillHighSpeedTele = new SkillHighSpeedTele(), 0);
		//this.addSkill(skillBrainTrainingCourse2 = new SkillBrainTrainingCourse2(), 0);
		//this.addSkill(skillSpiritTrainingCourse = new SkillSpiritTrainingCourse(), 0);
		
		this.setLogo("tp/main.png");
	}

	@Override
	public String getInternalName() {
		return "teleport";
	}
	
	//Static util functions
	/**
	 * Get the level of two string theorys' sum. Used in both error calculation and CP consumption.
	 */
	public static int getStringTheoryLevel(AbilityData data) {
		return 6;
	}
	
	/**
	 * Return the position or error modifier determined by buff skills.
	 */
	public static double getErrorModifier(AbilityData data) {
		return 0.5 * (1.0 - getStringTheoryLevel(data) * 0.053);
	}
}

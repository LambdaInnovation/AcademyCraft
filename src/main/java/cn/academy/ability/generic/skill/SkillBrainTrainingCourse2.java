package cn.academy.ability.generic.skill;

import cn.academy.api.ability.SkillBase;

public class SkillBrainTrainingCourse2 extends SkillBase {
	public SkillBrainTrainingCourse2() {
		this.setName("gn_btc2");
		this.setLogo("generic/brain_training_course2.png");
	}
	
	@Override
	public boolean isDummy() {
		return true;
	}
}

package cn.academy.ability.generic.skill;

import cn.academy.api.ability.SkillBase;

public class SkillBrainTrainingCourse extends SkillBase {

	public SkillBrainTrainingCourse() {
		this.setName("gn_btc");
		this.setLogo("generic/brain_training_course.png");
	}
	
	@Override
	public boolean isDummy() {
		return true;
	}
}

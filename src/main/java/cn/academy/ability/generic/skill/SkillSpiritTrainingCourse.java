package cn.academy.ability.generic.skill;

import cn.academy.api.ability.SkillBase;

public class SkillSpiritTrainingCourse extends SkillBase {
	public SkillSpiritTrainingCourse() {
		setName("gn_spirit");
		setLogo("generic/spirit_training_course.png");
	}
	
	@Override
	public boolean isDummy() {
		return true;
	}
}

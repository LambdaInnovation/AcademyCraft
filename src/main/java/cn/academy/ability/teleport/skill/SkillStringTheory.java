package cn.academy.ability.teleport.skill;

import cn.academy.api.ability.SkillBase;

public class SkillStringTheory extends SkillBase {
	public SkillStringTheory() {
		setName("tp_string");
		setLogo("tp/string_theory.png");
	}
	
	@Override
	public boolean isDummy() {
		return true;
	}
}

package cn.academy.api.ability;

import cn.academy.api.data.AbilityData;
import net.minecraft.util.StatCollector;

public class Level {
	
	Category parent;
	public int id;
	
	public Level(Category cat) {
		parent = cat;
	}
	
	public int getInitialCP() {
		return 0;
	}
	
	public boolean canLearnSkill(SkillBase skill) {
		return false;
	}
	
	public int getID() {
		return id;
	}
	
	public String getDisplayName() {
		return StatCollector.translateToLocal("level_" + parent.getCategoryId() + "_" + getID());
	}
	
	public void enterLevel(AbilityData abilityData) {
	}
}

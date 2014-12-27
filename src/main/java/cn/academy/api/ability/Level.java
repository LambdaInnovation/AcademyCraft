package cn.academy.api.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.academy.api.data.AbilityData;
import net.minecraft.util.StatCollector;

public abstract class Level {
	
	private Category parent;
	protected int id;
	
	private Map<Integer, Boolean> canLeranSkillMap = new HashMap<Integer, Boolean>();
	
	public Level(Category cat) {
		parent = cat;
	}
	
	public final boolean addCanLearnSkill(int skillId) {
		canLeranSkillMap.put(skillId, true);
		return true;
	}
	
	public final boolean canLearnSkill(int skillId) {
		Boolean canLearn = canLeranSkillMap.get(skillId);
		return canLearn != null ? canLearn : false;
	}
	
	public final List<Integer> getCanLearnSkillIdList() {
		return new ArrayList<Integer>(canLeranSkillMap.keySet());
	}
	
	public int getID() {
		return id;
	}
	public abstract int getLevelNum();
	
	public abstract float getInitialCP();
	
	public abstract float getMaxCP();
	
	public abstract float getInitRecoverCPRate();
	
	public abstract float getMaxRecoverCPRate();
	
	public String getDisplayName() {
		return StatCollector.translateToLocal("level_" + parent.getCategoryId() + "_" + getID());
	}
	
	public void enterLevel(AbilityData abilityData) {
	}
}

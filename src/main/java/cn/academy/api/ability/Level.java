package cn.academy.api.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.academy.api.data.AbilityData;
import net.minecraft.util.StatCollector;

public class Level {
	
	private Category parent;
	protected int id;
	
	private int levelNum;
	private float initialCP;
	private float maxCP;
	private float initRecoverCPRate;
	private float maxRecoverCPRate;
	
	private Map<Integer, Boolean> canLeranSkillMap = new HashMap<Integer, Boolean>();
	
	public Level(Category cat, int levelNum, float initialCP, float maxCP, float initRecoverCPRate, float maxRecoverCPRate) {
		this.parent = cat;
		this.levelNum = levelNum;
		this.initialCP = initialCP;
		this.maxCP = maxCP;
		this.initRecoverCPRate = initRecoverCPRate;
		this.maxRecoverCPRate = maxRecoverCPRate;
	}
	
	public int getLevelNum() {
		return levelNum;
	}

	public float getInitialCP() {
		return initialCP;
	}

	public float getMaxCP() {
		return maxCP;
	}

	public float getInitRecoverCPRate() {
		return initRecoverCPRate;
	}

	public float getMaxRecoverCPRate() {
		return maxRecoverCPRate;
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
	
	public String getDisplayName() {
		return StatCollector.translateToLocal("level_" + parent.getCategoryId() + "_" + getID());
	}
	
	public void enterLevel(AbilityData abilityData) {
	}
}

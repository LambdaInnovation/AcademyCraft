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
	
	private float initialCP;
	private float maxCP;
	private float initRecoverCPRate;
	private float maxRecoverCPRate;
	private double stimProb;
	
	private Map<Integer, Boolean> canLearnSkillMap = new HashMap<Integer, Boolean>();
	
	public Level(Category cat, float initialCP, float maxCP, float initRecoverCPRate, float maxRecoverCPRate,
			double learnProb) {
		this.parent = cat;
		this.id = cat.getLevelCount();
		this.initialCP = initialCP;
		this.maxCP = maxCP;
		this.initRecoverCPRate = initRecoverCPRate;
		this.maxRecoverCPRate = maxRecoverCPRate;
		stimProb = learnProb;
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
	
	/**
	 * Get the successful probability for each stimulation in AbilityDeveloper when updating to this level.
	 */
	public double getStimulationProb() {
		return stimProb;
	}

	@Deprecated
	public final void addCanLearnSkill(int skillId) {
		canLearnSkillMap.put(skillId, true);
	}
	
	public final boolean canLearnSkill(int skillId) {
		Boolean canLearn = canLearnSkillMap.get(skillId);
		return canLearn != null ? canLearn : false;
	}
	
	public final List<Integer> getCanLearnSkillIdList() {
		return new ArrayList<Integer>(canLearnSkillMap.keySet());
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

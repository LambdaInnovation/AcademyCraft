package cn.academy.api.ability;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.util.StatCollector;
import cn.academy.api.data.AbilityData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Ability level providing skill learn info
 */
public class Level {
	
	private Category parent;
	protected int id;
	
	private float initialCP;
	private float maxCP;
	private float initRecoverCPRate;
	private float maxRecoverCPRate;
	private double stimProb;
	
	private Set<Integer> canLearnSkills = new HashSet<Integer>();
	
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
	
	public int getID() {
		return id;
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

	public final void addCanLearnSkill(int skillId) {
		canLearnSkills.add(skillId);
	}
	
	public final boolean canLearnSkill(int skillId) {
		return canLearnSkills.contains(skillId);
	}
	
	public final List<Integer> getCanLearnSkillList() {
		return new ArrayList<Integer>(canLearnSkills);
	}
	
	public String getDisplayName() {
		return StatCollector.translateToLocal("level_" + getID());
	}
	
	public void enterLevel(AbilityData data) {
		data.setMaxCP(this.initialCP);
		data.setCurrentCP(this.initialCP);
	}
}

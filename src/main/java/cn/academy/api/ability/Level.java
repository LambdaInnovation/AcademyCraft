/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.api.ability;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.util.StatCollector;
import cn.academy.api.data.AbilityData;

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

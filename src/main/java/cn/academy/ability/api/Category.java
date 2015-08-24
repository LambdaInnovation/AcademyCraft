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
package cn.academy.ability.api;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.developer.DeveloperType;
import cn.academy.core.client.Resources;
import cn.liutils.util.helper.Color;

import com.google.common.collect.ImmutableList;

/**
 * @author WeAthFolD
 */
public class Category {

	private List<Skill> skillList = new ArrayList();
	private List<Controllable> ctrlList = new ArrayList();
	
	private final String name;
	
	int catID = -1;
	
	protected ResourceLocation icon;
	
	/**
	 * The color style of this category. Used in many various places for rendering.
	 */
	protected Color colorStyle = Color.WHITE();
	
	public Category(String _name) {
		name = _name;
		icon = Resources.getTexture("abilities/" + name + "/icon");
	}
	
	public Color getColorStyle() {
		return colorStyle;
	}
	
	public void addSkill(Skill skill) {
		// TODO Can remove when release
		if(getSkill(skill.getName()) != null)
			throw new RuntimeException("Duplicating skill " + skill.getName() + "!!");
		
		skillList.add(skill);
		addControllable(skill);
		
		skill.addedSkill(this, skillList.size() - 1);
	}
	
	public int getSkillID(Skill s) {
		return skillList.indexOf(s);
	}
	
	public int getSkillCount() {
		return skillList.size();
	}
	
	public Skill getSkill(int id) {
		return (id >= skillList.size() || id < 0) ? null : skillList.get(id);
	}
	
	public boolean containsSkill(Skill skill) {
		return skill == getSkill(skill.getID());
	}
	
	public Skill getSkill(String name) {
		for(Skill s : skillList)
			if(s.getName().equals(name))
				return s;
		return null;
	}
	
	/**
	 * Get an <b>immutable</b> list of skills in this category.
	 */
	public List<Skill> getSkillList() {
		return ImmutableList.copyOf(skillList);
	}
	
	public List<Skill> getSkillsOfLevel(int level) {
		List<Skill> ret = new ArrayList();
		for(Skill s : skillList)
			if(s.getLevel() == level)
				ret.add(s);
		return ret;
	}
	
	public int getCategoryID() {
		return catID;
	}
	
	public void addControllable(Controllable c) {
		ctrlList.add(c);
		c.addedControllable(this, ctrlList.size() - 1);
	}
	
	/**
	 * Get the controllable with given id.
	 */
	public Controllable getControllable(int id) {
		if(id < 0)
			return null;
		if(ctrlList.size() > id)
			return ctrlList.get(id);
		return null;
	}
	
	/**
	 * Get the immutable controllable list of this category.
	 */
	public List<Controllable> getControllableList() {
		return ImmutableList.copyOf(ctrlList);
	}
	
	public ResourceLocation getIcon() {
		return icon;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDisplayName() {
		return StatCollector.translateToLocal("ac.ability." + name + ".name");
	}
	
	// Learning API
	
	/**
	 * Used in judgment in skill tree UI & developer.
	 * @param data Ability Data of the player, is guaranteed to have the same category.
	 * @return Whether the player can level up currently
	 */
	public boolean canLevelUp(DeveloperType type, AbilityData data) {
		if(data.getLevel() == 5)
			return false;
		for(Skill s : getSkillsOfLevel(data.getLevel())) {
			if(s.canControl() && data.getSkillExp(s) < 0.66f)
				return false;
		}
		int nextlv = data.getLevel() + 1;
		if(nextlv == 1 || nextlv == 2)
			return type == DeveloperType.PORTABLE;
		if(nextlv == 3)
			return type == DeveloperType.NORMAL || type == DeveloperType.ADVANCED;
		if(nextlv == 4 || nextlv == 5)
			return type == DeveloperType.ADVANCED;
		return true;
	}
	
}

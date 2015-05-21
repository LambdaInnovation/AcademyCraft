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
import cn.academy.ability.api.ctrl.Controllable;
import cn.academy.generic.client.Resources;

import com.google.common.collect.ImmutableList;

/**
 * @author WeAthFolD
 */
public final class Category {

	List<Skill> skillList = new ArrayList();
	List<Controllable> ctrlList = new ArrayList();
	
	private final String name;
	
	int catID = -1;
	
	protected ResourceLocation icon;
	
	public Category(String _name) {
		name = _name;
		icon = Resources.getTexture("abilities/" + name + "/icon");
	}
	
	public void addSkill(Skill skill) {
		skillList.add(skill);
		addControllable(skill);
		
		skill.addedIntoCategory(this);
	}
	
	public int getCategoryID() {
		return catID;
	}
	
	public void addControllable(Controllable c) {
		ctrlList.add(c);
	}
	
	/**
	 * Internal call used majorly by Preset system. DO NOT CALL THIS!
	 */
	public int getControlID(Skill skill) {
		return ctrlList.indexOf(skill);
	}
	
	/**
	 * Internal call used majorly by Preset system. DO NOT CALL THIS!
	 */
	public Controllable getControllable(int id) {
		if(ctrlList.size() > id)
			return ctrlList.get(id);
		return null;
	}
	
	/**
	 * Internal call used majorly by Preset system. DO NOT CALL THIS!
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
	
}

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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.core.AcademyCraft;
import cn.academy.core.client.Resources;

/**
 * @see SpecialSkill
 * @author WeAthFolD
 */
public abstract class SubSkill extends Controllable {
	
	private SpecialSkill parent;
	
	public final String name;
	
	private String fullName;
	
	/**
	 * The icon of this SubSkill.
	 */
	protected ResourceLocation icon;
	
	int keyID = -1;
	
	public SubSkill(String _name) {
		name = _name;
		fullName = "<unknown>.<unknown>." + name;
	}
	
	public boolean isRemapped() {
		return keyID != -1;
	}
	
	/**
	 * Set this SubSkill to remap a certain keyboard key, rather than using normal ability key slots.
	 * @param defKeyID default key ID for this SubSkill. The key can be set in config file.
	 */
	public void setRemapped(int defKeyID) {
		keyID = defKeyID;
	}
	
	/**
	 * Called each time this SubSkill is activated. Return the remapped key of the skill (if it remaps).
	 */
	public int getRemappedKey() {
		return AcademyCraft.config.get("keys", getFullName(), keyID).getInt();
	}
	
	final void addedInto(SpecialSkill skill) {
		fullName = skill.getFullName() + "." + name;
		
		// academy:abilities/<category>/<parent>/<name>.png
		icon = Resources.getTexture(
			"abilities/" + skill.getCategory().getName() + "/" + 
			skill.getName() + "/" + name);
	}

	public SpecialSkill getParent() {
		return parent;
	}

	@Override
	public ResourceLocation getHintIcon() {
		return icon;
	}

	@Override
	public String getHintText() {
		return getLocalized("name");
	}
	
	public String getFullName() {
		return fullName;
	}
	
	protected String getLocalized(String key) {
		return StatCollector.translateToLocal("ac.ability." + getFullName() + "." + key);
	}

}

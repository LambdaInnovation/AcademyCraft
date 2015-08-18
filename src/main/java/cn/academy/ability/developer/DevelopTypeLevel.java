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
package cn.academy.ability.developer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import cn.academy.ability.api.Category;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.client.skilltree.SkillTreeLocal;
import cn.academy.core.AcademyCraft;
import cn.academy.core.client.Resources;

/**
 * @author WeAthFolD
 */
public class DevelopTypeLevel implements IDevelopType {
	
	static final ResourceLocation TEX_CATNF = Resources.getTexture("guis/skill_tree/cat_not_found");
	
	@Override
	public int getStimulations(EntityPlayer player) {
		AbilityData aData = AbilityData.get(player);
		return AcademyCraft.getScript().at("ac.ability.learning")
				.getFunction("uplevel_cost").callInteger(aData.getLevel() + 1);
	}

	@Override
	public DeveloperType getMinimumType(EntityPlayer player) {
		AbilityData aData = AbilityData.get(player);
		if(aData.getLevel() == 0)
			return DeveloperType.PORTABLE;
		if(aData.getLevel() <= 3)
			return DeveloperType.NORMAL;
		if(aData.getLevel() <= 5)
			return DeveloperType.ADVANCED;
		throw new IllegalStateException("What you think you can reach level 6?");
	}

	@Override
	public boolean validate(EntityPlayer player) {
		return LearningHelper.canLevelUp(AbilityData.get(player));
	}

	@Override
	public void onLearned(EntityPlayer player) {
		AbilityData aData = AbilityData.get(player);
		aData.setLevel(aData.getLevel() + 1);
	}

	@Override
	public ResourceLocation getIcon(EntityPlayer player) {
		Category cat = AbilityData.get(player).getCategory();
		return cat == null ? TEX_CATNF : cat.getIcon();
	}

	@Override
	public String getName(EntityPlayer player) {
		return SkillTreeLocal.upgradeTo(AbilityData.get(player).getLevel() + 1);
	}

}

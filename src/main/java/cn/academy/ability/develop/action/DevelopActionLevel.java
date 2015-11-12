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
package cn.academy.ability.develop.action;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.CategoryManager;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.client.skilltree.SkillTreeLocal;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.ability.develop.LearningHelper;
import cn.academy.core.AcademyCraft;
import cn.academy.core.client.Resources;
import cn.lambdalib.util.generic.RandUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 */
public class DevelopActionLevel implements IDevelopAction {
	
	static final ResourceLocation TEX_CATNF = Resources.getTexture("guis/skill_tree/cat_not_found");
	
	@Override
	public int getStimulations(EntityPlayer player) {
		AbilityData aData = AbilityData.get(player);
		return AcademyCraft.getScript().at("ac.ability.learning")
				.getFunction("uplevel_cost").callInteger(aData.getLevel() + 1);
	}

	@Override
	public boolean validate(EntityPlayer player, IDeveloper developer) {
		return LearningHelper.canLevelUp(developer.getType(), AbilityData.get(player));
	}

	@Override
	public void onLearned(EntityPlayer player) {
		AbilityData aData = AbilityData.get(player);
		if(aData.getCategory() == null) {
			// WELCOME TO THE WORLD OF ESPER! >)
			CategoryManager man = CategoryManager.INSTANCE;
			Category cat = man.getCategory(RandUtils.nextInt(man.getCategoryCount()));
			aData.setCategory(cat);
		} else
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

/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.develop.action;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.CategoryManager;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.client.AbilityLocalization;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.ability.develop.LearningHelper;
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
        return 5 * (AbilityData.get(player).getLevel() + 1);
    }

    @Override
    public boolean validate(EntityPlayer player, IDeveloper developer) {
        return LearningHelper.canLevelUp(developer.getType(), AbilityData.get(player));
    }

    @Override
    public void onLearned(EntityPlayer player) {
        AbilityData aData = AbilityData.get(player);
        if(!aData.hasCategory()) {
            // WELCOME TO THE WORLD OF ESPER! >)
            CategoryManager man = CategoryManager.INSTANCE;
            Category cat = man.getCategory(RandUtils.nextInt(man.getCategoryCount()));
            aData.setCategory(cat);
        } else
            aData.setLevel(aData.getLevel() + 1);
    }

    @Override
    public ResourceLocation getIcon(EntityPlayer player) {
        AbilityData adata = AbilityData.get(player);
        return adata.hasCategory() ? adata.getCategory().getIcon() : TEX_CATNF;
    }

    @Override
    public String getName(EntityPlayer player) {
        return AbilityLocalization.instance.upgradeTo(AbilityData.get(player).getLevel() + 1);
    }

}

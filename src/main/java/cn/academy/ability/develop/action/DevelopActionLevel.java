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
import cn.academy.ability.item.ItemInductionFactor;
import cn.academy.core.client.Resources;
import cn.lambdalib.util.generic.RandUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author WeAthFolD
 */
public class DevelopActionLevel implements IDevelopAction {

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
            aData.setCategory(chooseCategory(player));
        } else {
            aData.setLevel(aData.getLevel() + 1);
        }
    }

    private Category chooseCategory(EntityPlayer player) {
        Optional<ItemStack> inductedCategory = DevelopActionReset.getFactor(player);
        if (inductedCategory.isPresent()) {
            ItemStack factor = inductedCategory.get();
            int factorIdx = Arrays.asList(player.inventory.mainInventory).indexOf(factor);
            player.inventory.mainInventory[factorIdx] = null;
            return ItemInductionFactor.getCategory(factor);
        } else {
            CategoryManager man = CategoryManager.INSTANCE;
            return man.getCategory(RandUtils.nextInt(man.getCategoryCount()));
        }
    }

}

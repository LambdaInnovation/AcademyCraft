package cn.academy.ability.develop.action;

import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.datapart.AbilityData;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.ability.develop.LearningHelper;
import cn.academy.item.ItemInductionFactor;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.Collections;
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
            int factorIdx = player.inventory.mainInventory.indexOf(factor);
            player.inventory.setInventorySlotContents(factorIdx, ItemStack.EMPTY);
            return ItemInductionFactor.getCategory(factor);
        } else {
            CategoryManager man = CategoryManager.INSTANCE;
            return man.getCategory(RandUtils.nextInt(man.getCategoryCount()));
        }
    }

}
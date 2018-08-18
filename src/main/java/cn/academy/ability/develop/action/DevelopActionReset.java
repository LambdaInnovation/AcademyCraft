package cn.academy.ability.develop.action;

import cn.academy.ACItems;
import cn.academy.ability.Category;
import cn.academy.datapart.AbilityData;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.item.ItemInductionFactor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

public class DevelopActionReset implements IDevelopAction {

    public static boolean canReset(EntityPlayer player, IDeveloper developer) {
        AbilityData data = AbilityData.get(player);
        ItemStack equip = player.getHeldItemMainhand();
        Optional<ItemStack> factor = getFactor(player);

        int level = data.getLevel();

        return level >= 3 &&
                developer.getType() == DeveloperType.ADVANCED &&
                equip != null && equip.getItem() == ACItems.magnetic_coil &&
                factor.isPresent() && ItemInductionFactor.getCategory(factor.get()) != data.getCategory();
    }

    static Optional<ItemStack> getFactor(EntityPlayer player) {
        Category playerCategory = AbilityData.get(player).getCategoryNullable();
        return player.inventory.mainInventory.parallelStream()
                .filter(stack -> stack != null && stack.getItem() instanceof ItemInductionFactor)
                .filter(stack -> ItemInductionFactor.getCategory(stack) != playerCategory)
                .findAny();
    }

    @Override
    public int getStimulations(EntityPlayer player) {
        AbilityData data = AbilityData.get(player);
        return data.getLevel() * 10;
    }

    @Override
    public boolean validate(EntityPlayer player, IDeveloper developer) {
        return canReset(player, developer);
    }

    @Override
    public void onLearned(EntityPlayer player) {
        AbilityData data = AbilityData.get(player);

        ItemStack factor = getFactor(player).get();

        Category newCat = ItemInductionFactor.getCategory(factor);

        int prevLevel = data.getLevel();

        data.setCategory(newCat);
        data.setLevel(prevLevel - 1);

        player.setHeldItem(EnumHand.MAIN_HAND, null);

        int factorIdx = Collections.singletonList(player.inventory.mainInventory).indexOf(factor);
        player.inventory.mainInventory.set(factorIdx, null);
    }
}
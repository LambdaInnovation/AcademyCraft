package cn.academy.item;

import cn.academy.ACItems;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.block.block.ACFluids;
import cn.academy.support.EnergyItemHelper;
import cn.lambdalib2.registry.StateEventCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.loot.conditions.RandomChanceWithLooting;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

public class ACItemAdditionalRegistry {

    @StateEventCallback
    private static void init(FMLInitializationEvent ev) {
        // TODO use loot table
        String[] factorAppearance = { MINESHAFT_CORRIDOR, PYRAMID_DESERT_CHEST, PYRAMID_JUNGLE_CHEST, STRONGHOLD_LIBRARY,
            DUNGEON_CHEST };

        // TODO test generation density
        for (String s : factorAppearance) {
            for (Category c : CategoryManager.INSTANCE.getCategories()) {
                ItemStack stack = ACItems.induction_factor.create(c);
                ChestGenHooks.addItem(s, new WeightedRandomChestContent(stack, 1, 1, 4));
            }
        }
    }

}

package cn.li.mc1211.shim;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import clojure.lang.IFn;

/**
 * Adapter that accepts Clojure callbacks and accepts recipes into a RecipeOutput.
 * Replaces the removed FinishedRecipe JSON-emitter model from 1.20.1.
 */
public final class DelegatingFinishedRecipe {
    private DelegatingFinishedRecipe() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void accept(RecipeOutput output, ResourceLocation id, Recipe<?> recipe, IFn advancementFn) {
        if (advancementFn == null) {
            output.accept(id, recipe, null);
        } else {
            Object adv = advancementFn.invoke(id);
            output.accept(id, (Recipe) recipe, adv instanceof net.minecraft.advancements.AdvancementHolder
                ? (net.minecraft.advancements.AdvancementHolder) adv
                : null);
        }
    }
}

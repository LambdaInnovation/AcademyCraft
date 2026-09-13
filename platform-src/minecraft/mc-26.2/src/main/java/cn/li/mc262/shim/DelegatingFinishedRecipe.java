package cn.li.mc262.shim;

import clojure.lang.IFn;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

/**
 * Accept a recipe into RecipeOutput. 26.2 uses ResourceKey&lt;Recipe&lt;?&gt;&gt; instead of Identifier.
 */
public final class DelegatingFinishedRecipe {
    private DelegatingFinishedRecipe() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void accept(RecipeOutput output, Identifier id, Recipe<?> recipe, IFn advancementFn) {
        if (output == null || id == null || recipe == null) {
            return;
        }
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
        AdvancementHolder advancement = null;
        if (advancementFn != null) {
            Object adv = advancementFn.invoke(id);
            if (adv instanceof AdvancementHolder holder) {
                advancement = holder;
            }
        }
        output.accept(key, (Recipe) recipe, advancement);
    }

    public static void accept(RecipeOutput output, ResourceKey<Recipe<?>> key, Recipe<?> recipe) {
        if (output == null || key == null || recipe == null) {
            return;
        }
        output.accept(key, recipe, null);
    }
}

package cn.li.mc262.shim;

import clojure.lang.IFn;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.core.registries.Registries;

/**
 * 26.2 RecipeProvider: buildRecipes() takes no args; registration goes through
 * {@link RecipeProvider.Runner} + GatherDataEvent.createProvider / addProvider.
 */
public class DelegatingRecipeProvider extends RecipeProvider {
    private final IFn buildRecipesFn;

    public DelegatingRecipeProvider(HolderLookup.Provider registries,
                                    RecipeOutput output,
                                    IFn buildRecipesFn) {
        super(registries, output);
        this.buildRecipesFn = buildRecipesFn;
    }

    @Override
    public void buildRecipes() {
        if (buildRecipesFn != null) {
            // Pass provider so Clojure can read items/output fields via bridges.
            buildRecipesFn.invoke(this);
        }
    }

    /** Exposed for Clojure: HolderGetter&lt;Item&gt; used by shaped/shapeless builders. */
    public net.minecraft.core.HolderGetter<net.minecraft.world.item.Item> itemLookup() {
        return this.registries.lookupOrThrow(Registries.ITEM);
    }

    /** Exposed for Clojure: RecipeOutput stored by the parent constructor. */
    public RecipeOutput recipeOutput() {
        return this.output;
    }

    /**
     * Runner registered with the data generator; resolves lookups then constructs
     * the real provider.
     */
    public static final class Runner extends RecipeProvider.Runner {
        private final IFn buildRecipesFn;

        public Runner(PackOutput packOutput,
                      CompletableFuture<HolderLookup.Provider> registries,
                      IFn buildRecipesFn) {
            super(packOutput, registries);
            this.buildRecipesFn = buildRecipesFn;
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries,
                                                      RecipeOutput output) {
            return new DelegatingRecipeProvider(registries, output, buildRecipesFn);
        }

        @Override
        public String getName() {
            return "academy_recipes";
        }
    }
}

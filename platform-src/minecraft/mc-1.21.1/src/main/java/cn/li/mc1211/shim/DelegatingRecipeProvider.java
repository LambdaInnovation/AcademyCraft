package cn.li.mc1211.shim;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import clojure.lang.IFn;

import java.util.concurrent.CompletableFuture;

/** Universal RecipeProvider skeleton for Minecraft 1.21.1 RecipeOutput. */
public class DelegatingRecipeProvider extends RecipeProvider {

    private final IFn buildRecipesFn;

    /** Fabric's provider factory only exposes PackOutput; keep the legacy
     * two-argument adapter while recipes remain registry-independent. */
    public DelegatingRecipeProvider(PackOutput packOutput, IFn buildRecipesFn) {
        this(packOutput, CompletableFuture.completedFuture(null), buildRecipesFn);
    }

    public DelegatingRecipeProvider(PackOutput packOutput,
                                    CompletableFuture<HolderLookup.Provider> registries,
                                    IFn buildRecipesFn) {
        super(packOutput, registries);
        this.buildRecipesFn = buildRecipesFn;
    }

    @Override
    public void buildRecipes(RecipeOutput output) {
        if (buildRecipesFn != null) {
            buildRecipesFn.invoke(this, output);
        }
    }
}

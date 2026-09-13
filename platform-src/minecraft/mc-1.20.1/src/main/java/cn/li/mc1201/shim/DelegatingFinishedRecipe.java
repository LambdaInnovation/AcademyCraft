package cn.li.mc1201.shim;

import com.google.gson.JsonObject;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import clojure.lang.IFn;

/** Universal FinishedRecipe skeleton — replaces all proxy sites
 *  implementing FinishedRecipe. */
public class DelegatingFinishedRecipe implements FinishedRecipe {

    private final IFn getIdFn;
    private final IFn getTypeFn;
    private final IFn serializeRecipeFn;
    private final IFn serializeAdvancementFn;
    private final IFn getAdvancementIdFn;

    public DelegatingFinishedRecipe(IFn getIdFn, IFn getTypeFn, IFn serializeRecipeFn,
                                    IFn serializeAdvancementFn, IFn getAdvancementIdFn) {
        this.getIdFn = getIdFn;
        this.getTypeFn = getTypeFn;
        this.serializeRecipeFn = serializeRecipeFn;
        this.serializeAdvancementFn = serializeAdvancementFn;
        this.getAdvancementIdFn = getAdvancementIdFn;
    }

    @Override public ResourceLocation getId() {
        return (ResourceLocation) getIdFn.invoke();
    }

    @Override public RecipeSerializer<?> getType() {
        return (RecipeSerializer<?>) getTypeFn.invoke();
    }

    @Override public void serializeRecipeData(JsonObject json) {
        // All data is serialized via serializeRecipe() returning the full JsonObject.
        // Called by the default serializeRecipe(JsonObject) method; no-op here.
    }

    @Override public JsonObject serializeRecipe() {
        return (JsonObject) serializeRecipeFn.invoke();
    }

    @Override public JsonObject serializeAdvancement() {
        return (JsonObject) serializeAdvancementFn.invoke();
    }

    @Override public ResourceLocation getAdvancementId() {
        return (ResourceLocation) getAdvancementIdFn.invoke();
    }
}

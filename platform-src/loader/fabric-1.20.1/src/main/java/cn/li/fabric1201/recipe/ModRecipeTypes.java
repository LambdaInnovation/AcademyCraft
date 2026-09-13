package cn.li.fabric1201.recipe;

import cn.li.mcver.ResourceLocations;

import cn.li.mc1201.recipe.ContentRecipe;
import cn.li.mcmod.ModId;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Fabric registration for content machine recipe types/serializers.
 * Installs suppliers into {@link ContentRecipe} for shared query/datagen.
 */
public final class ModRecipeTypes {

    public static final RecipeType<ContentRecipe> CONTENT_PROCESS_TYPE =
        registerType("content_process");

    public static final RecipeType<ContentRecipe> CONTENT_MODE_TYPE =
        registerType("content_mode");

    public static final RecipeSerializer<ContentRecipe> CONTENT_PROCESS_SERIALIZER =
        Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            id("content_process"),
            new ContentRecipe.Serializer("process"));

    public static final RecipeSerializer<ContentRecipe> CONTENT_MODE_SERIALIZER =
        Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            id("content_mode"),
            new ContentRecipe.Serializer("mode"));

    static {
        ContentRecipe.install(
            () -> CONTENT_PROCESS_TYPE,
            () -> CONTENT_MODE_TYPE,
            () -> CONTENT_PROCESS_SERIALIZER,
            () -> CONTENT_MODE_SERIALIZER
        );
    }

    /** Force class init / registry side effects during mod bootstrap. */
    public static void register() {
        // static block performs registration
    }

    private static RecipeType<ContentRecipe> registerType(String path) {
        return Registry.register(
            BuiltInRegistries.RECIPE_TYPE,
            id(path),
            new RecipeType<ContentRecipe>() {
                @Override
                public String toString() {
                    return ModId.ID + ":" + path;
                }
            });
    }

    private static ResourceLocation id(String path) {
        return ResourceLocations.of(ModId.ID, path);
    }

    private ModRecipeTypes() {}
}

package cn.li.neoforge262.recipe;

import cn.li.mc262.recipe.ContentRecipe;
import cn.li.neoforge262.AcademyCraft262;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Custom recipe types/serializers for content machines (process / mode).
 */
public final class ModRecipeTypes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, AcademyCraft262.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, AcademyCraft262.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ContentRecipe>> CONTENT_PROCESS_TYPE =
            RECIPE_TYPES.register("content_process",
                    () -> RecipeType.simple(Identifier.fromNamespaceAndPath(AcademyCraft262.MODID, "content_process")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ContentRecipe>> CONTENT_MODE_TYPE =
            RECIPE_TYPES.register("content_mode",
                    () -> RecipeType.simple(Identifier.fromNamespaceAndPath(AcademyCraft262.MODID, "content_mode")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ContentRecipe>> CONTENT_PROCESS_SERIALIZER =
            RECIPE_SERIALIZERS.register("content_process", () -> ContentRecipe.createSerializer("process"));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ContentRecipe>> CONTENT_MODE_SERIALIZER =
            RECIPE_SERIALIZERS.register("content_mode", () -> ContentRecipe.createSerializer("mode"));

    static {
        ContentRecipe.install(
                CONTENT_PROCESS_TYPE::get,
                CONTENT_MODE_TYPE::get,
                CONTENT_PROCESS_SERIALIZER::get,
                CONTENT_MODE_SERIALIZER::get
        );
    }

    public static void register(IEventBus modBus) {
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
    }

    private ModRecipeTypes() {
    }
}

package cn.li.neoforge1211.recipe;

import cn.li.mc1211.recipe.ContentRecipe;
import cn.li.neoforge1211.AcademyCraft1211;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Custom recipe types and serializers for Imaginary Fusor and Metal Former.
 *
 * Both RecipeType and RecipeSerializer use DeferredRegister —
 * never call vanilla RecipeType.register() directly, which writes
 * to the locked vanilla registry and throws IllegalStateException.
 */
public final class ModRecipeTypes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(Registries.RECIPE_TYPE, AcademyCraft1211.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, AcademyCraft1211.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ContentRecipe>> CONTENT_PROCESS_TYPE =
        RECIPE_TYPES.register("content_process", () -> new RecipeType<ContentRecipe>() {
            @Override
            public String toString() {
                return AcademyCraft1211.MODID + ":content_process";
            }
        });

    public static final DeferredHolder<RecipeType<?>, RecipeType<ContentRecipe>> CONTENT_MODE_TYPE =
        RECIPE_TYPES.register("content_mode", () -> new RecipeType<ContentRecipe>() {
            @Override
            public String toString() {
                return AcademyCraft1211.MODID + ":content_mode";
            }
        });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ContentRecipe>> CONTENT_PROCESS_SERIALIZER =
        RECIPE_SERIALIZERS.register("content_process", () -> new ContentRecipe.Serializer("process"));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ContentRecipe>> CONTENT_MODE_SERIALIZER =
        RECIPE_SERIALIZERS.register("content_mode", () -> new ContentRecipe.Serializer("mode"));

    static {
        ContentRecipe.install(
            CONTENT_PROCESS_TYPE::get,
            CONTENT_MODE_TYPE::get,
            CONTENT_PROCESS_SERIALIZER::get,
            CONTENT_MODE_SERIALIZER::get
        );
    }

    /**
     * Register both DeferredRegisters with the mod event bus.
     * Called from Clojure during mod construction.
     */
    public static void register(IEventBus modBus) {
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
    }

    private ModRecipeTypes() {}
}

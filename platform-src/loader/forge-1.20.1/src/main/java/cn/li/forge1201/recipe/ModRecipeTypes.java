package cn.li.forge1201.recipe;

import cn.li.forge1201.AcademyCraft1201;
import cn.li.mc1201.recipe.ContentRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Custom recipe types and serializers for Imaginary Fusor and Metal Former.
 *
 * Both RecipeType and RecipeSerializer use DeferredRegister (Forge-standard
 * pattern) — never call vanilla RecipeType.register() directly, which writes
 * to the locked vanilla registry and throws IllegalStateException.
 */
public final class ModRecipeTypes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(Registries.RECIPE_TYPE, AcademyCraft1201.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.RECIPE_SERIALIZERS, AcademyCraft1201.MODID);

    public static final RegistryObject<RecipeType<ContentRecipe>> CONTENT_PROCESS_TYPE =
        RECIPE_TYPES.register("content_process", () -> new RecipeType<ContentRecipe>() {
            @Override
            public String toString() {
                return AcademyCraft1201.MODID + ":content_process";
            }
        });

    public static final RegistryObject<RecipeType<ContentRecipe>> CONTENT_MODE_TYPE =
        RECIPE_TYPES.register("content_mode", () -> new RecipeType<ContentRecipe>() {
            @Override
            public String toString() {
                return AcademyCraft1201.MODID + ":content_mode";
            }
        });

    public static final RegistryObject<RecipeSerializer<ContentRecipe>> CONTENT_PROCESS_SERIALIZER =
        RECIPE_SERIALIZERS.register("content_process", () -> new ContentRecipe.Serializer("process"));

    public static final RegistryObject<RecipeSerializer<ContentRecipe>> CONTENT_MODE_SERIALIZER =
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

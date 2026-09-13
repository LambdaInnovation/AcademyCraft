package cn.li.forge1201.worldgen;

import cn.li.forge1201.AcademyCraft1201;
import cn.li.mcbase.clj.ClojureInterop;
import cn.li.mc1201.worldgen.ConfigurablePoolFeature;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * Registry for custom world generation features.
 * Upstream implementation had genOres + genPoolLiquid worldgen config.
 * Configurable pool feature fill block is injected via mcmod worldgen registry.
 */
public class ModFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
        DeferredRegister.create(Registries.FEATURE, AcademyCraft1201.MODID);

    /**
     * Configurable pool feature. Resolves the registered fill block at
     * feature instantiation time (after block registration) so we don't need
     * Java-level access to the Clojure-registered block class.
     * Block ID is read from the mcmod platform-neutral worldgen registry.
     */
    private static final Supplier<Block> POOL_FILL_SUPPLIER = () -> {
        ClojureInterop.requireNamespace("cn.li.mcmod.worldgen");
        Object result = ClojureInterop.invoke("cn.li.mcmod.worldgen", "get-pool-fill-block-id");
        String blockId = result instanceof String s ? s : null;
        Block block = blockId != null ? BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId)) : null;
        return block != null ? block : Blocks.WATER;
    };

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> CONFIGURABLE_POOL =
        FEATURES.register("configurable_pool", () ->
            new ConfigurablePoolFeature(NoneFeatureConfiguration.CODEC,
                () -> POOL_FILL_SUPPLIER.get().defaultBlockState()));

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}

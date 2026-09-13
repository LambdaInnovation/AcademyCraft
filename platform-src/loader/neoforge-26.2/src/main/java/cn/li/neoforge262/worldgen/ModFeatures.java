package cn.li.neoforge262.worldgen;

import cn.li.neoforge262.AcademyCraft262;
import cn.li.mcbase.clj.ClojureInterop;
import cn.li.mc262.worldgen.ConfigurablePoolFeature;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registry for custom world generation features.
 * Upstream implementation had genOres + genPoolLiquid worldgen config.
 * Configurable pool feature fill block is injected via mcmod worldgen registry.
 */
public class ModFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
        DeferredRegister.create(Registries.FEATURE, AcademyCraft262.MODID);

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
        Block block = blockId != null ? BuiltInRegistries.BLOCK.getValue(Identifier.parse(blockId)) : null;
        return block != null ? block : Blocks.WATER;
    };

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> CONFIGURABLE_POOL =
        FEATURES.register("configurable_pool", () ->
            new ConfigurablePoolFeature(NoneFeatureConfiguration.CODEC,
                () -> POOL_FILL_SUPPLIER.get().defaultBlockState()));

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}

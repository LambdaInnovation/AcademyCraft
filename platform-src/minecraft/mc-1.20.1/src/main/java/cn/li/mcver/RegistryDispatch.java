package cn.li.mcver;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/**
 * Runtime registration into BuiltInRegistries without Clojure reflection.
 * Id type comes from {@link ResourceLocations#of}.
 */
public final class RegistryDispatch {
    private RegistryDispatch() {
    }

    public static Block registerBlock(String namespace, String path, Block instance) {
        return Registry.register(BuiltInRegistries.BLOCK, ResourceLocations.of(namespace, path), instance);
    }

    public static Item registerItem(String namespace, String path, Item instance) {
        return Registry.register(BuiltInRegistries.ITEM, ResourceLocations.of(namespace, path), instance);
    }

    public static Fluid registerFluid(String namespace, String path, Fluid instance) {
        return Registry.register(BuiltInRegistries.FLUID, ResourceLocations.of(namespace, path), instance);
    }
}

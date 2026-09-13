package cn.li.mcver;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;

/**
 * Cross-version BuiltInRegistries lookups.
 * 26.2: getValue.
 */
public final class RegistryValues {
    private RegistryValues() {
    }

    @Nullable
    public static Item getItem(Identifier id) {
        if (id == null) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        return item == null || item == Items.AIR ? null : item;
    }

    @Nullable
    public static Block getBlock(Identifier id) {
        if (id == null) {
            return null;
        }
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        return block == null || block == Blocks.AIR ? null : block;
    }

    @Nullable
    public static ParticleType<?> getParticleType(Identifier id) {
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.PARTICLE_TYPE.getValue(id);
    }
}

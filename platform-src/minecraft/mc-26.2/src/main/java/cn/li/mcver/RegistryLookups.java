package cn.li.mcver;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Cross-version registry holder lookup (registryOrThrow vs lookupOrThrow).
 */
public final class RegistryLookups {
    private RegistryLookups() {
    }

    public static <T> Holder<T> holderOrThrow(Level level, ResourceKey<T> key) {
        return level.registryAccess().lookupOrThrow(key.registryKey()).getOrThrow(key);
    }
}

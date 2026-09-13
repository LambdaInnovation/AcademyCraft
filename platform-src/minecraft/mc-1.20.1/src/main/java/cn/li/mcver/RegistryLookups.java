package cn.li.mcver;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Cross-version registry holder lookup (registryOrThrow vs lookupOrThrow).
 */
public final class RegistryLookups {
    private RegistryLookups() {
    }

    @SuppressWarnings("unchecked")
    public static <T> Holder<T> holderOrThrow(Level level, ResourceKey<T> key) {
        ResourceKey<? extends Registry<?>> registryKey =
                ResourceKey.createRegistryKey(key.registry());
        return level.registryAccess()
                .registryOrThrow((ResourceKey<? extends Registry<? extends T>>) registryKey)
                .getHolderOrThrow(key);
    }
}

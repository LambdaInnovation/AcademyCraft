package cn.li.mcbase.block.entity;

import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared registry for scripted block entity types.
 *
 * <p>Loaders register their platform-specific ScriptedBlockEntity
 * types via this shared utility to avoid duplication of registry logic.</p>
 */
public final class BlockEntityRegistry {

    private BlockEntityRegistry() {
    }

    private static final Map<String, BlockEntityType<?>> TYPES = new HashMap<>();

    public static void registerType(String tileId, BlockEntityType<?> type) {
        TYPES.put(tileId, type);
    }

    public static BlockEntityType<?> getType(String tileId) {
        return TYPES.get(tileId);
    }

    /**
     * All registered scripted block-entity types (for NeoForge capability provider registration).
     */
    public static Collection<BlockEntityType<?>> allTypes() {
        return Collections.unmodifiableCollection(TYPES.values());
    }

    public static void clear() {
        TYPES.clear();
    }
}

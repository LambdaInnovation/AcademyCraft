package cn.li.mc1201.block.entity;

import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Collection;

/**
 * @deprecated Use {@link cn.li.mcbase.block.entity.BlockEntityRegistry}.
 */
@Deprecated
public final class BlockEntityRegistry {
    private BlockEntityRegistry() {
    }

    public static void registerType(String tileId, BlockEntityType<?> type) {
        cn.li.mcbase.block.entity.BlockEntityRegistry.registerType(tileId, type);
    }

    public static BlockEntityType<?> getType(String tileId) {
        return cn.li.mcbase.block.entity.BlockEntityRegistry.getType(tileId);
    }

    public static Collection<BlockEntityType<?>> allTypes() {
        return cn.li.mcbase.block.entity.BlockEntityRegistry.allTypes();
    }

    public static void clear() {
        cn.li.mcbase.block.entity.BlockEntityRegistry.clear();
    }
}

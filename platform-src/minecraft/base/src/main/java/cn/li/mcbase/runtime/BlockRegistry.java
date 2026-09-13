package cn.li.mcbase.runtime;

import cn.li.mcver.RegistryValues;
import cn.li.mcver.ResourceLocations;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Shared BuiltInRegistries block lookups via {@link RegistryValues}.
 */
public final class BlockRegistry {
    private BlockRegistry() {
    }

    public static Block getBlockById(String blockId) {
        try {
            if (blockId == null || blockId.isEmpty()) {
                return null;
            }
            return RegistryValues.getBlock(ResourceLocations.parse(blockId));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static Block findBlock(String namespace, String path) {
        try {
            if (namespace == null || namespace.isEmpty() || path == null || path.isEmpty()) {
                return null;
            }
            return RegistryValues.getBlock(ResourceLocations.of(namespace, path));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String getBlockKey(Object block) {
        if (!(block instanceof Block b)) {
            return null;
        }
        var key = BuiltInRegistries.BLOCK.getKey(b);
        return key != null ? key.toString() : null;
    }

    public static boolean isAirBlock(Block block, Block airBlock) {
        return block == null || block == airBlock;
    }

    public static Block getAirBlock() {
        return Blocks.AIR;
    }

    public static Block blockByItem(Item item) {
        if (item == null) {
            return null;
        }
        return Block.byItem(item);
    }

    public static boolean isPlaceableBlockItem(Object item) {
        if (!(item instanceof Item i)) {
            return false;
        }
        Block block = blockByItem(i);
        return block != null && block != Blocks.AIR;
    }
}

package cn.li.mc1201.runtime;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * @deprecated Use {@link cn.li.mcbase.runtime.BlockRegistry}.
 */
@Deprecated
public final class BlockRegistry {
    private BlockRegistry() {
    }

    public static Block getBlockById(String blockId) {
        return cn.li.mcbase.runtime.BlockRegistry.getBlockById(blockId);
    }

    public static Block findBlock(String namespace, String path) {
        return cn.li.mcbase.runtime.BlockRegistry.findBlock(namespace, path);
    }

    public static String getBlockKey(Object block) {
        return cn.li.mcbase.runtime.BlockRegistry.getBlockKey(block);
    }

    public static boolean isAirBlock(Block block, Block airBlock) {
        return cn.li.mcbase.runtime.BlockRegistry.isAirBlock(block, airBlock);
    }

    public static Block getAirBlock() {
        return cn.li.mcbase.runtime.BlockRegistry.getAirBlock();
    }

    public static Block blockByItem(Item item) {
        return cn.li.mcbase.runtime.BlockRegistry.blockByItem(item);
    }

    public static boolean isPlaceableBlockItem(Object item) {
        return cn.li.mcbase.runtime.BlockRegistry.isPlaceableBlockItem(item);
    }
}

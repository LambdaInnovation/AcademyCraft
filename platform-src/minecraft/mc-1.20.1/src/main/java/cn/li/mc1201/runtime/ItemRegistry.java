package cn.li.mc1201.runtime;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * @deprecated Use {@link cn.li.mcbase.runtime.ItemRegistry}.
 */
@Deprecated
public final class ItemRegistry {
    static {
        // Force shared static init (ItemPlayerOps install).
        cn.li.mcbase.runtime.ItemRegistry.class.getName();
    }

    private ItemRegistry() {
    }

    public static Item getItemById(String itemId) {
        return cn.li.mcbase.runtime.ItemRegistry.getItemById(itemId);
    }

    public static String getItemRegistryPath(Item item) {
        return cn.li.mcbase.runtime.ItemRegistry.getItemRegistryPath(item);
    }

    public static String getItemKey(Item item) {
        return cn.li.mcbase.runtime.ItemRegistry.getItemKey(item);
    }

    public static ItemStack createItemStackById(String itemId, int count) {
        return cn.li.mcbase.runtime.ItemRegistry.createItemStackById(itemId, count);
    }
}

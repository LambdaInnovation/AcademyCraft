package cn.li.mcbase.runtime;

import cn.li.mcver.RegistryValues;
import cn.li.mcver.ResourceLocations;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Shared BuiltInRegistries item lookups via {@link RegistryValues}.
 */
public final class ItemRegistry {
    static {
        ItemPlayerOps.installItemIdLookup(ItemRegistry::getItemById);
    }

    private ItemRegistry() {
    }

    public static Item getItemById(String itemId) {
        try {
            if (itemId == null || itemId.isEmpty()) {
                return null;
            }
            return RegistryValues.getItem(ResourceLocations.parse(itemId));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String getItemRegistryPath(Item item) {
        if (item == null) {
            return null;
        }
        var key = BuiltInRegistries.ITEM.getKey(item);
        return key != null ? key.getPath() : null;
    }

    public static String getItemKey(Item item) {
        if (item == null) {
            return null;
        }
        var key = BuiltInRegistries.ITEM.getKey(item);
        return key != null ? key.toString() : null;
    }

    public static ItemStack createItemStackById(String itemId, int count) {
        Item item = getItemById(itemId);
        if (item == null || count <= 0) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
    }
}

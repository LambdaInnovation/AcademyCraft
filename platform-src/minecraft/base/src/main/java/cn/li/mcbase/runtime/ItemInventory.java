package cn.li.mcbase.runtime;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Shared player inventory helpers.
 * Uses {@link Inventory#getContainerSize()}/{@link Inventory#getItem(int)} so
 * 26.2 (no public items/offhand lists) and classic share one loop.
 */
public final class ItemInventory {
    private ItemInventory() {
    }

    public static boolean isItemStackEmpty(Object stack) {
        return stack == null || ((ItemStack) stack).isEmpty();
    }

    public static Object getItemFromStack(Object stack) {
        return ((ItemStack) stack).getItem();
    }

    public static int getItemStackCount(Object stack) {
        return ((ItemStack) stack).getCount();
    }

    public static String getItemKeyString(Object itemOrStack) {
        Item item;
        if (itemOrStack instanceof ItemStack stack) {
            item = stack.getItem();
        } else if (itemOrStack instanceof Item i) {
            item = i;
        } else {
            return null;
        }
        var key = BuiltInRegistries.ITEM.getKey(item);
        return key != null ? key.toString() : null;
    }

    public static int countPlayerItem(Player player, Item item) {
        if (player == null || item == null) {
            return 0;
        }
        int total = 0;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static boolean consumePlayerItem(Player player, Item item, int amount) {
        if (player == null || item == null || amount <= 0) {
            return false;
        }
        int remaining = amount;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (remaining <= 0) {
                break;
            }
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
        return remaining <= 0;
    }

    public static boolean givePlayerItemStack(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return false;
        }
        ItemStack copy = stack.copy();
        if (!player.getInventory().add(copy)) {
            player.drop(copy, false);
        }
        return true;
    }
}

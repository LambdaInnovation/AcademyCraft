package cn.li.mcbase.runtime;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Player item count/consume/give helpers. Item id resolution is installed per MC version.
 */
public final class ItemPlayerOps {
    @FunctionalInterface
    public interface ItemIdLookup {
        Item getItemById(String itemId);
    }

    private static volatile ItemIdLookup LOOKUP;

    private ItemPlayerOps() {
    }

    public static void installItemIdLookup(ItemIdLookup lookup) {
        LOOKUP = lookup;
    }

    private static ItemIdLookup lookup() {
        ItemIdLookup local = LOOKUP;
        if (local == null) {
            throw new IllegalStateException("ItemPlayerOps ItemIdLookup not installed");
        }
        return local;
    }

    public static int countPlayerItemById(Object playerObj, String itemId) {
        if (!(playerObj instanceof Player player) || itemId == null || itemId.isEmpty()) {
            return 0;
        }
        Item item = lookup().getItemById(itemId);
        if (item == null) {
            return 0;
        }
        return countPlayerItem(player, item);
    }

    public static boolean consumePlayerItemById(Object playerObj, String itemId, int amount) {
        if (!(playerObj instanceof Player player) || itemId == null || itemId.isEmpty() || amount <= 0) {
            return false;
        }
        Item item = lookup().getItemById(itemId);
        if (item == null) {
            return false;
        }
        return consumePlayerItem(player, item, amount);
    }

    public static boolean givePlayerItemStack(Object playerObj, Object stackObj) {
        if (!(playerObj instanceof Player player) || !(stackObj instanceof ItemStack stack) || stack.isEmpty()) {
            return false;
        }
        return givePlayerItemStack(player, stack);
    }

    private static int countPlayerItem(Player player, Item item) {
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

    private static boolean consumePlayerItem(Player player, Item item, int amount) {
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

    private static boolean givePlayerItemStack(Player player, ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!player.getInventory().add(copy)) {
            player.drop(copy, false);
        }
        return true;
    }
}

package cn.li.mc1201.runtime;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * @deprecated Use {@link cn.li.mcbase.runtime.ItemInventory}.
 */
@Deprecated
public final class ItemInventory {
    private ItemInventory() {
    }

    public static boolean isItemStackEmpty(Object stack) {
        return cn.li.mcbase.runtime.ItemInventory.isItemStackEmpty(stack);
    }

    public static Object getItemFromStack(Object stack) {
        return cn.li.mcbase.runtime.ItemInventory.getItemFromStack(stack);
    }

    public static int getItemStackCount(Object stack) {
        return cn.li.mcbase.runtime.ItemInventory.getItemStackCount(stack);
    }

    public static String getItemKeyString(Object itemOrStack) {
        return cn.li.mcbase.runtime.ItemInventory.getItemKeyString(itemOrStack);
    }

    public static int countPlayerItem(Player player, Item item) {
        return cn.li.mcbase.runtime.ItemInventory.countPlayerItem(player, item);
    }

    public static boolean consumePlayerItem(Player player, Item item, int amount) {
        return cn.li.mcbase.runtime.ItemInventory.consumePlayerItem(player, item, amount);
    }

    public static boolean givePlayerItemStack(Player player, ItemStack stack) {
        return cn.li.mcbase.runtime.ItemInventory.givePlayerItemStack(player, stack);
    }
}

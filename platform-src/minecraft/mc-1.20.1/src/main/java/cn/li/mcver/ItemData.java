package cn.li.mcver;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Version seam for item custom data.
 * Contract is shaped for Data Components (1.20.5+); 1.20.1 maps onto CompoundTag.
 */
public final class ItemData {
    private ItemData() {
    }

    public static boolean hasCustomData(ItemStack stack) {
        return stack != null && stack.hasTag();
    }

    public static CompoundTag getCustomDataCopy(ItemStack stack) {
        if (stack == null || !stack.hasTag()) {
            return new CompoundTag();
        }
        return stack.getTag().copy();
    }

    public static CompoundTag getOrCreateCustomData(ItemStack stack) {
        return stack.getOrCreateTag();
    }

    public static void setCustomData(ItemStack stack, CompoundTag data) {
        if (stack == null) {
            return;
        }
        if (data == null || data.isEmpty()) {
            stack.setTag(null);
        } else {
            stack.setTag(data);
        }
    }

    public static void removeCustomData(ItemStack stack) {
        if (stack != null) {
            stack.setTag(null);
        }
    }
}

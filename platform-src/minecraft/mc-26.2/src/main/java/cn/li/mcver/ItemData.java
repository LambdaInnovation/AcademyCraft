package cn.li.mcver;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Version seam for item custom data via Data Components
 * ({@link DataComponents#CUSTOM_DATA} + {@link CustomData}).
 * 26.2 kept this API unchanged from 1.21.1.
 *
 * <p>{@link #getOrCreateCustomData} returns a mutable copy; callers must
 * {@link #setCustomData} after mutating to persist changes.
 */
public final class ItemData {
    private ItemData() {
    }

    public static boolean hasCustomData(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && !data.isEmpty();
    }

    public static CompoundTag getCustomDataCopy(ItemStack stack) {
        if (stack == null) {
            return new CompoundTag();
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) {
            return new CompoundTag();
        }
        return data.copyTag();
    }

    /**
     * Returns a mutable CompoundTag copy of the stack's custom data
     * (empty if absent). Persist with {@link #setCustomData}.
     */
    public static CompoundTag getOrCreateCustomData(ItemStack stack) {
        if (stack == null) {
            return new CompoundTag();
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) {
            return new CompoundTag();
        }
        return data.copyTag();
    }

    public static void setCustomData(ItemStack stack, CompoundTag data) {
        if (stack == null) {
            return;
        }
        if (data == null || data.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
    }

    public static void removeCustomData(ItemStack stack) {
        if (stack != null) {
            stack.remove(DataComponents.CUSTOM_DATA);
        }
    }
}

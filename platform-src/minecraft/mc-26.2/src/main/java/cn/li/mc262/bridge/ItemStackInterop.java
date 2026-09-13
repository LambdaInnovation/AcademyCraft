package cn.li.mc262.bridge;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** 26.2 ItemStack helpers (hover name + codec save). */
public final class ItemStackInterop {
    private ItemStackInterop() {}

    public static ItemStack setHoverName(ItemStack stack, Component name) {
        if (stack != null && name != null) {
            stack.set(DataComponents.CUSTOM_NAME, name);
        }
        return stack;
    }

    /** Encode stack via {@link ItemStack#CODEC} into a CompoundTag (empty on failure). */
    public static CompoundTag saveToTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }
        return ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack)
            .result()
            .filter(CompoundTag.class::isInstance)
            .map(CompoundTag.class::cast)
            .orElseGet(CompoundTag::new);
    }

    /** Decode a stack written by {@link #saveToTag(ItemStack)}. */
    public static ItemStack loadFromTag(Object data) {
        if (!(data instanceof CompoundTag tag) || tag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ItemStack.CODEC.parse(NbtOps.INSTANCE, tag)
            .result()
            .orElse(ItemStack.EMPTY);
    }

    public static void mergeSavedInto(ItemStack stack, CompoundTag data) {
        if (data == null) {
            return;
        }
        CompoundTag saved = saveToTag(stack);
        if (!saved.isEmpty()) {
            for (String key : saved.keySet()) {
                Tag value = saved.get(key);
                if (value != null) {
                    data.put(key, value.copy());
                }
            }
        }
    }
}

package cn.li.mc1211.bridge;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** 1.21 ItemStack name helpers (setHoverName removed). */
public final class ItemStackInterop {
    private ItemStackInterop() {
    }

    public static ItemStack setHoverName(ItemStack stack, Component name) {
        stack.set(DataComponents.CUSTOM_NAME, name);
        return stack;
    }
}

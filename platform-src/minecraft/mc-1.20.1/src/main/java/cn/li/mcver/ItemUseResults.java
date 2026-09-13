package cn.li.mcver;

import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;

/**
 * Cross-version Item.use return values.
 * Classic: InteractionResultHolder; 26.2: InteractionResult.
 */
public final class ItemUseResults {
    private ItemUseResults() {
    }

    public static InteractionResultHolder<ItemStack> success(ItemStack stack) {
        return InteractionResultHolder.success(stack);
    }

    public static InteractionResultHolder<ItemStack> pass(ItemStack stack) {
        return InteractionResultHolder.pass(stack);
    }
}

package cn.li.mcver;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

/**
 * Cross-version Item.use return values.
 * Classic: InteractionResultHolder; 26.2: InteractionResult.
 */
public final class ItemUseResults {
    private ItemUseResults() {
    }

    public static InteractionResult success(ItemStack stack) {
        return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }

    public static InteractionResult pass(ItemStack stack) {
        return InteractionResult.PASS;
    }
}

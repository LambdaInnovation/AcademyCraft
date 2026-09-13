package cn.li.mcver;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/**
 * Cross-version ItemStack enchant helpers.
 * 1.20.1: classic Enchantment instance API.
 */
public final class ItemStackEnchants {
    private ItemStackEnchants() {
    }

    public static ItemStack fortuneNetheritePickaxe(Level level, int fortuneLevel) {
        ItemStack stack = new ItemStack(Items.NETHERITE_PICKAXE);
        stack.enchant(Enchantments.BLOCK_FORTUNE, fortuneLevel);
        return stack;
    }
}

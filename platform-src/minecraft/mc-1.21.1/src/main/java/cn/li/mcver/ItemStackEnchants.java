package cn.li.mcver;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/**
 * Cross-version ItemStack enchant helpers.
 * 1.21.1: Holder-based Enchantments/FORTUNE.
 */
public final class ItemStackEnchants {
    private ItemStackEnchants() {
    }

    public static ItemStack fortuneNetheritePickaxe(Level level, int fortuneLevel) {
        ItemStack stack = new ItemStack(Items.NETHERITE_PICKAXE);
        Holder<Enchantment> holder = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.FORTUNE);
        stack.enchant(holder, fortuneLevel);
        return stack;
    }
}

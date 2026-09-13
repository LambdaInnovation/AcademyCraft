package cn.li.mcver;

import net.minecraft.core.HolderGetter;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nullable;

/**
 * Cross-version Ingredient construction.
 * Classic: Ingredient.of(ItemLike...) / of(TagKey);
 * 26.2: tag ingredients require HolderGetter.
 */
public final class Ingredients {
    private Ingredients() {
    }

    public static Ingredient ofItem(ItemLike item) {
        return Ingredient.of(item);
    }

    public static Ingredient ofTag(TagKey<Item> tag, @Nullable HolderGetter<Item> items) {
        return Ingredient.of(tag);
    }
}

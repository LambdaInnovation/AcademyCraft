package cn.li.mcver;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nullable;

/**
 * Cross-version Ingredient construction.
 * 26.2: tag ingredients require HolderGetter&lt;Item&gt;.
 */
public final class Ingredients {
    private Ingredients() {
    }

    public static Ingredient ofItem(ItemLike item) {
        return Ingredient.of(item);
    }

    public static Ingredient ofTag(TagKey<Item> tag, @Nullable HolderGetter<Item> items) {
        if (items == null) {
            throw new IllegalArgumentException("Tag ingredient requires HolderGetter<Item> on 26.2");
        }
        HolderSet.Named<Item> holders = items.getOrThrow(tag);
        return Ingredient.of(holders);
    }
}

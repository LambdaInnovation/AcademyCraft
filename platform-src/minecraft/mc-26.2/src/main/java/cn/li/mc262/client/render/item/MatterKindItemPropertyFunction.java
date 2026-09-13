package cn.li.mc262.client.render.item;

import cn.li.mcver.ItemData;
import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;

/**
 * Numeric item-model property for the matter-unit material kind, read from
 * ItemStack damage (upstream ItemMatterUnit indexes its materials by
 * getItemDamage: 0 = empty, 1 = imag phase liquid). Registered as
 * {@code academy:matter_kind} for {@code :item-model-damage-frame} items —
 * NOT the vanilla {@code minecraft:damage} property, which divides by
 * maxDamage and is unusable on stackable items (matter units have no
 * durability).
 */
public enum MatterKindItemPropertyFunction implements RangeSelectItemModelProperty {
    INSTANCE;

    public static final MapCodec<MatterKindItemPropertyFunction> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        if (ItemData.hasCustomData(stack)) {
            CompoundTag tag = ItemData.getCustomDataCopy(stack);
            if ("phase-liquid".equals(tag.getStringOr("matterKind", ""))) {
                return 1.0F;
            }
        }
        return stack.getDamageValue() >= 1 ? 1.0F : 0.0F;
    }

    @Override
    public MapCodec<? extends RangeSelectItemModelProperty> type() {
        return CODEC;
    }
}

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
 * Numeric item-model property for the energy ratio (0–1) stored in custom data.
 */
public enum EnergyItemPropertyFunction implements RangeSelectItemModelProperty {
    INSTANCE;

    public static final MapCodec<EnergyItemPropertyFunction> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        if (!ItemData.hasCustomData(stack)) {
            return 0.0F;
        }
        CompoundTag tag = ItemData.getCustomDataCopy(stack);
        double current = tag.getDoubleOr("energy", 0.0D);
        double maximum = tag.getDoubleOr("maxEnergy", 1.0D);
        if (maximum <= 0.0D || current <= 0.0D) {
            return 0.0F;
        }
        return current >= maximum ? 1.0F : (float) (current / maximum);
    }

    @Override
    public MapCodec<? extends RangeSelectItemModelProperty> type() {
        return CODEC;
    }
}

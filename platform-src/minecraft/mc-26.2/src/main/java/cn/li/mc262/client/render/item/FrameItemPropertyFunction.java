package cn.li.mc262.client.render.item;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;

/**
 * Numeric item-model property for the frame animation of filled matter units
 * (upstream ItemMatterUnit: {@code frame = (int)(GameTimer.getTime()*4) % 4}
 * — the flowing-liquid texture advances one frame every 0.25s, 4 frames per
 * second). Registered as {@code academy:frame} for
 * {@code :item-model-damage-frame} items.
 */
public enum FrameItemPropertyFunction implements RangeSelectItemModelProperty {
    INSTANCE;

    public static final MapCodec<FrameItemPropertyFunction> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        // Item models can be resolved outside a ClientLevel and cached across
        // world ticks. A wall-clock frame is available in every display
        // context and matches upstream's GameTimer-based 4 FPS animation.
        return (System.currentTimeMillis() / 250L) % 4L;
    }

    @Override
    public MapCodec<? extends RangeSelectItemModelProperty> type() {
        return CODEC;
    }
}

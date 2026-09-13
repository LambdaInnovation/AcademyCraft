package cn.li.mc1211.client.render.item;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Item model predicate for the frame animation of filled matter units
 * (upstream ItemMatterUnit: {@code frame = (int)(GameTimer.getTime()*4) % 4}
 * — the flowing-liquid texture advances one frame every 0.25s, 4 frames per
 * second). Registered as {@code <modid>:frame} for
 * {@code :item-model-damage-frame} items.
 */
public enum FrameItemPropertyFunction implements ItemPropertyFunction {
    INSTANCE;

    @Override
    public float call(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        if (level == null) {
            return 0.0F;
        }
        // Upstream: frame = (int)(GameTimer.getTime() * 4) % 4 — one frame
        // every 0.25s, the full 4-frame loop per second. Game time advances
        // 20 ticks/s, so a 20-tick cycle at 5 ticks/frame matches it.
        return (level.getGameTime() % 20) / 5.0F;
    }
}

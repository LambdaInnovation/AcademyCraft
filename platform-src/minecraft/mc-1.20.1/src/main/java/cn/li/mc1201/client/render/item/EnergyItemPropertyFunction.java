package cn.li.mc1201.client.render.item;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Item model predicate for energy ratio (0–1) from NBT {@code energy}/{@code maxEnergy}.
 * Used with generated content item model overrides ({@code <modid>:energy}).
 */
public enum EnergyItemPropertyFunction implements ItemPropertyFunction {
    INSTANCE;

    @Override
    public float call(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return 0.0F;
        }
        double cur = tag.getDouble("energy");
        double mx = tag.getDouble("maxEnergy");
        if (mx <= 0.0D) {
            mx = 1.0D;
        }
        if (cur <= 0.0D) {
            return 0.0F;
        }
        if (cur >= mx) {
            return 1.0F;
        }
        return (float) (cur / mx);
    }
}

package cn.li.mc1201.client.render.item;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Item model predicate for the matter-unit material kind, read from
 * ItemStack damage (upstream ItemMatterUnit indexes its materials by
 * getItemDamage: 0 = empty, 1 = imag phase liquid). Registered as
 * {@code <modid>:matter_kind} for {@code :item-model-damage-frame} items —
 * NOT the vanilla {@code minecraft:damage} predicate, which divides by
 * maxDamage and is unusable on stackable items (matter units have no
 * durability).
 */
public enum MatterKindItemPropertyFunction implements ItemPropertyFunction {
    INSTANCE;

    @Override
    public float call(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        return stack.getDamageValue() >= 1 ? 1.0F : 0.0F;
    }
}

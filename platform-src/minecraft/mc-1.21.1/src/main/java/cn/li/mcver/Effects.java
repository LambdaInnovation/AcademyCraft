package cn.li.mcver;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Version seam for potion effects.
 * 1.21.1 APIs are Holder-native; lookup helpers resolve registry holders.
 */
public final class Effects {
    private Effects() {
    }

    /**
     * Resolve a registered effect to its registry {@link Holder}.
     * Returns null if the effect is not in {@link BuiltInRegistries#MOB_EFFECT}.
     */
    public static Holder<MobEffect> holderOf(MobEffect effect) {
        if (effect == null) {
            return null;
        }
        return BuiltInRegistries.MOB_EFFECT
            .getResourceKey(effect)
            .flatMap(BuiltInRegistries.MOB_EFFECT::getHolder)
            .orElse(null);
    }

    public static Holder<MobEffect> holderOf(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
    }

    public static MobEffect unwrap(Holder<MobEffect> holder) {
        return holder == null ? null : holder.value();
    }

    public static boolean hasEffect(LivingEntity entity, Holder<MobEffect> effect) {
        return entity != null && effect != null && entity.hasEffect(effect);
    }

    public static MobEffectInstance getEffect(LivingEntity entity, Holder<MobEffect> effect) {
        if (entity == null || effect == null) {
            return null;
        }
        return entity.getEffect(effect);
    }

    public static boolean addEffect(LivingEntity entity, Holder<MobEffect> effect, int duration, int amplifier) {
        if (entity == null || effect == null) {
            return false;
        }
        return entity.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }

    public static boolean removeEffect(LivingEntity entity, Holder<MobEffect> effect) {
        if (entity == null || effect == null) {
            return false;
        }
        return entity.removeEffect(effect);
    }
}

package cn.li.mcver;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Version seam for potion effects.
 * 26.2 keeps effects Holder-native; {@code Registry#getHolder} was removed
 * from the base {@code Registry} interface, so lookups go through
 * {@link net.minecraft.core.Registry#wrapAsHolder} (always succeeds, falling
 * back to a direct holder for unregistered values) and
 * {@link net.minecraft.core.Registry#get(Identifier)}.
 */
public final class Effects {
    private Effects() {
    }

    /**
     * Resolve a registered effect to its registry {@link Holder}.
     * Never returns null for a non-null effect: unregistered values fall
     * back to a direct holder via {@code wrapAsHolder}.
     */
    public static Holder<MobEffect> holderOf(MobEffect effect) {
        if (effect == null) {
            return null;
        }
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }

    public static Holder<MobEffect> holderOf(Identifier id) {
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.MOB_EFFECT.get(id)
            .<Holder<MobEffect>>map(holder -> holder)
            .orElse(null);
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

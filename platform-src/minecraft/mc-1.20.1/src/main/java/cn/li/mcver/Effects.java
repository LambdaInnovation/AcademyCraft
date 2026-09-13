package cn.li.mcver;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Version seam for potion effects.
 * Contract is shaped around Holder&lt;MobEffect&gt; (1.20.5+); 1.20.1 wraps bare MobEffect.
 */
public final class Effects {
    private Effects() {
    }

    public static Holder<MobEffect> holderOf(MobEffect effect) {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }

    public static Holder<MobEffect> holderOf(ResourceLocation id) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
        if (effect == null) {
            return null;
        }
        return holderOf(effect);
    }

    public static MobEffect unwrap(Holder<MobEffect> holder) {
        return holder == null ? null : holder.value();
    }

    public static boolean hasEffect(LivingEntity entity, Holder<MobEffect> effect) {
        return entity != null && effect != null && entity.hasEffect(unwrap(effect));
    }

    public static MobEffectInstance getEffect(LivingEntity entity, Holder<MobEffect> effect) {
        if (entity == null || effect == null) {
            return null;
        }
        return entity.getEffect(unwrap(effect));
    }

    public static boolean addEffect(LivingEntity entity, Holder<MobEffect> effect, int duration, int amplifier) {
        if (entity == null || effect == null) {
            return false;
        }
        return entity.addEffect(new MobEffectInstance(unwrap(effect), duration, amplifier));
    }

    public static boolean removeEffect(LivingEntity entity, Holder<MobEffect> effect) {
        if (entity == null || effect == null) {
            return false;
        }
        return entity.removeEffect(unwrap(effect));
    }
}

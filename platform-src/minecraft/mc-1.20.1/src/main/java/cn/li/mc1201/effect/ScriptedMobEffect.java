package cn.li.mc1201.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class ScriptedMobEffect extends MobEffect {
    private final int tickInterval;
    private final float damagePerTick;

    public ScriptedMobEffect(MobEffectCategory category,
                             int color,
                             int tickInterval,
                             float damagePerTick) {
        super(category, color);
        this.tickInterval = Math.max(1, tickInterval);
        this.damagePerTick = Math.max(0.0f, damagePerTick);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (damagePerTick > 0.0f && entity.isAlive()) {
            float damage = damagePerTick * (amplifier + 1);
            entity.hurt(entity.damageSources().magic(), damage);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % tickInterval == 0;
    }
}
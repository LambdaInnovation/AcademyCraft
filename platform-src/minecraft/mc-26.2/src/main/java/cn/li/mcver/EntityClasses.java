package cn.li.mcver;

import cn.li.mc262.entity.ScriptedEffectEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

/**
 * Version-local entity implementation classes for shared Clojure cores.
 */
public final class EntityClasses {
    private EntityClasses() {
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends Entity> scriptedEffectEntity() {
        return (Class<? extends Entity>) (Class<?>) ScriptedEffectEntity.class;
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends Entity> abstractArrow() {
        return (Class<? extends Entity>) (Class<?>) AbstractArrow.class;
    }
}

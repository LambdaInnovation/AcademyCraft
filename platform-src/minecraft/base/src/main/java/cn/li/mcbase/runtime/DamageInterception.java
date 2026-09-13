package cn.li.mcbase.runtime;

import clojure.lang.IFn;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Loader-neutral bridge used by native event adapters and the Fabric mixin.
 * Content damage semantics remain in the common Clojure runtime.
 */
public final class DamageInterception {
    private static volatile IFn modifier;

    private DamageInterception() {
    }

    public static void installModifier(IFn modifierFn) {
        modifier = modifierFn;
    }

    public static float modifyDamage(LivingEntity entity, DamageSource source, float amount) {
        IFn current = modifier;
        if (current == null || entity == null || source == null || !Float.isFinite(amount)) {
            return amount;
        }
        try {
            Object result = current.invoke(entity, source, (double) amount);
            if (result instanceof Number number) {
                float modified = number.floatValue();
                return Float.isFinite(modified) ? modified : amount;
            }
        } catch (Throwable ignored) {
            // Damage hooks must fail open even for foreign-mod linkage errors.
        }
        return amount;
    }

    public static void rewriteArmorInput(LivingEntity entity, Args args) {
        DamageSource source = args.get(0);
        float amount = args.get(1);
        args.set(1, modifyDamage(entity, source, amount));
    }
}

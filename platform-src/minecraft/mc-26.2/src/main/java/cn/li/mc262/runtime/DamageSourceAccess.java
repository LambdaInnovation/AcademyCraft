package cn.li.mc262.runtime;

/**
 * @deprecated Use {@link cn.li.mcbase.runtime.DamageSourceAccess}.
 */
@Deprecated
public final class DamageSourceAccess {
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> VEC_REFLECTION =
            cn.li.mcbase.runtime.DamageSourceAccess.VEC_REFLECTION;

    private DamageSourceAccess() {
    }

    public static boolean isVecReflection(net.minecraft.world.damagesource.DamageSource source) {
        return cn.li.mcbase.runtime.DamageSourceAccess.isVecReflection(source);
    }

    public static net.minecraft.world.damagesource.DamageSource resolve(
            net.minecraft.world.level.Level level, String kind) {
        return cn.li.mcbase.runtime.DamageSourceAccess.resolve(level, kind);
    }

    public static net.minecraft.world.damagesource.DamageSource resolveKeyword(
            net.minecraft.world.level.Level level, Object sourceType) {
        return cn.li.mcbase.runtime.DamageSourceAccess.resolveKeyword(level, sourceType);
    }

    public static net.minecraft.world.damagesource.DamageSource resolveKeyword(
            net.minecraft.world.level.Level level, Object sourceType, Object attacker) {
        return cn.li.mcbase.runtime.DamageSourceAccess.resolveKeyword(level, sourceType, attacker);
    }
}

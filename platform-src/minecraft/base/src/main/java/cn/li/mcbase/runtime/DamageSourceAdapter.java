package cn.li.mcbase.runtime;

/**
 * Loader-agnostic adapter seam for damage source construction.
 */
public abstract class DamageSourceAdapter {
    public abstract Object createGenericDamageSource(Object level, Object attacker, String kind);
}

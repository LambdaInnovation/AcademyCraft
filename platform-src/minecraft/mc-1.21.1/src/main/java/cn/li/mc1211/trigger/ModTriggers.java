package cn.li.mc1211.trigger;

/**
 * Custom advancement trigger holder (Minecraft 1.21.1).
 *
 * <p>Does <em>not</em> call {@code CriteriaTriggers.register} — in 1.21.1 the
 * trigger_type registry is frozen by mod-construct time. Loaders must register
 * {@link #CUSTOM} via DeferredRegister / RegisterEvent (see NeoForge
 * {@code ModCriterionTriggers}).
 */
public final class ModTriggers {

    public static final ModCustomTrigger CUSTOM = new ModCustomTrigger();

    private ModTriggers() {}

    /** Ensure class loading; registration is owned by the loader. */
    public static void init() {
        // no-op
    }
}

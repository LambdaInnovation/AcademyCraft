package cn.li.mc262.trigger;

/**
 * Custom advancement trigger holder (Minecraft 26.2).
 *
 * <p>Does not call vanilla CriteriaTriggers.register — loaders register
 * {@link #CUSTOM} via DeferredRegister (see NeoForge ModCriterionTriggers).
 */
public final class ModTriggers {

    public static final ModCustomTrigger CUSTOM = new ModCustomTrigger();

    private ModTriggers() {
    }

    public static void init() {
        // no-op; class load ensures CUSTOM exists for DeferredRegister
    }
}

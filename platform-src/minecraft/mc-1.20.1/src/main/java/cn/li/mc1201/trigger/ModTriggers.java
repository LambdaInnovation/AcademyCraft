package cn.li.mc1201.trigger;

import net.minecraft.advancements.CriteriaTriggers;

/**
 * Custom trigger registry (vanilla CriteriaTriggers).
 *
 * <p>Only registers generic Minecraft trigger objects. Game-specific mapping
 * is owned by AC namespaces.
 */
public final class ModTriggers {

    public static final ModCustomTrigger CUSTOM = CriteriaTriggers.register(new ModCustomTrigger());

    private ModTriggers() {}

    /** Ensure class loading and static registration. */
    public static void init() {
        // no-op
    }
}

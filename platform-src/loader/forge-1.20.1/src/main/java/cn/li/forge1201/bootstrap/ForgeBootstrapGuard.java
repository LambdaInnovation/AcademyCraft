package cn.li.forge1201.bootstrap;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JVM-static bootstrap guards that survive Clojure namespace reload during dev.
 */
public final class ForgeBootstrapGuard {

    private static final AtomicBoolean LIFECYCLE_INITIALIZED = new AtomicBoolean(false);
    private static final AtomicBoolean MOD_BUS_REGISTERED = new AtomicBoolean(false);
    private static final AtomicBoolean COMMON_SETUP_COMPLETE = new AtomicBoolean(false);

    private ForgeBootstrapGuard() {
    }

    public static boolean markLifecycleInitializedIfAbsent() {
        return LIFECYCLE_INITIALIZED.compareAndSet(false, true);
    }

    public static boolean markModBusRegisteredIfAbsent() {
        return MOD_BUS_REGISTERED.compareAndSet(false, true);
    }

    public static boolean markCommonSetupCompleteIfAbsent() {
        return COMMON_SETUP_COMPLETE.compareAndSet(false, true);
    }

    public static boolean commonSetupComplete() {
        return COMMON_SETUP_COMPLETE.get();
    }
}

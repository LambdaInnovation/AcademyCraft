package cn.li.neoforgebase.event;

import clojure.java.api.Clojure;
import clojure.lang.Var;
import cn.li.mcbase.clj.ClojureInterop;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

/**
 * Shared NeoForge command registration bridge.
 * Callers pass their loader commands namespace (the {@code *.commands} ns for that target).
 */
public final class ForgeCommandRegistrar {
    private ForgeCommandRegistrar() {
    }

    public static void registerAll(RegisterCommandsEvent event, Logger logger, String commandsNs) {
        try {
            ClojureInterop.requireNamespace(commandsNs);

            Var handler = (Var) Clojure.var(commandsNs, "register-all-commands");
            if (!handler.isBound()) {
                throw new IllegalStateException("register-all-commands is unbound after require");
            }

            handler.invoke(event.getDispatcher(), event.getBuildContext());
        } catch (Throwable t) {
            logger.error("[ForgeEventBusManager] Failed to register commands", t);
        }
    }
}

package cn.li.forge1201.event;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Var;
import cn.li.mcbase.clj.ClojureInterop;
import net.minecraftforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

final class ForgeCommandRegistrar {
    private ForgeCommandRegistrar() {
    }

    static void registerAll(RegisterCommandsEvent event, Logger logger) {
        try {
            ClojureInterop.requireNamespace("cn.li.forge1201.commands");

            Var handler = (Var) Clojure.var("cn.li.forge1201.commands", "register-all-commands");
            if (!handler.isBound()) {
                throw new IllegalStateException("register-all-commands is unbound after require");
            }

            handler.invoke(event.getDispatcher(), event.getBuildContext());
        } catch (Throwable t) {
            logger.error("[ForgeEventBusManager] Failed to register commands", t);
        }
    }
}

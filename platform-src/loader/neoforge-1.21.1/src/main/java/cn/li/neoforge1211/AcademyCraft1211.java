package cn.li.neoforge1211;

import cn.li.mcmod.ModId;
import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Var;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Minimal Java bridge for @Mod annotation.
 * All logic implemented in cn.li.neoforge1211.mod Clojure namespace.
 */
@Mod(AcademyCraft1211.MODID)
public class AcademyCraft1211 {
    public static final String MODID = ModId.ID;

    public AcademyCraft1211(IEventBus modEventBus, ModContainer modContainer) {
        try {
            IFn require = Clojure.var("clojure.core", "require");
            Object modNs = Clojure.read("cn.li.neoforge1211.mod");
            synchronized (clojure.lang.RT.REQUIRE_LOCK) {
                require.invoke(modNs);
            }

            Var startFn = (Var) Clojure.var("cn.li.neoforge1211.mod", "start-neoforge-mod!");
            if (!startFn.isBound()) {
                // Fall back to the copied Forge entry name during the port.
                startFn = (Var) Clojure.var("cn.li.neoforge1211.mod", "start-forge-mod!");
            }
            if (!startFn.isBound()) {
                throw new IllegalStateException("Clojure bootstrap var is unbound: cn.li.neoforge1211.mod/start-neoforge-mod!");
            }
            startFn.invoke(modEventBus, modContainer);
        } catch (Throwable t) {
            System.err.println("Failed to load Clojure mod implementation:");
            t.printStackTrace();
            throw new RuntimeException("Clojure mod initialization failed — terminating Minecraft startup", t);
        }
    }
}

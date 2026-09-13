package cn.li.forge1201;

import cn.li.mcmod.ModId;
import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Var;
import net.minecraftforge.fml.common.Mod;

/**
 * Minimal Java bridge for @Mod annotation.
 * All logic implemented in cn.li.forge1201.mod Clojure namespace.
 */
@Mod(AcademyCraft1201.MODID)
public class AcademyCraft1201 {
    public static final String MODID = ModId.ID;

    public AcademyCraft1201() {
        // Load and instantiate the Clojure mod class for both normal run and datagen.
        // Datagen also needs platform registration state for official Forge model providers.
        try {
            IFn require = Clojure.var("clojure.core", "require");
            Object modNs = Clojure.read("cn.li.forge1201.mod");
            synchronized (clojure.lang.RT.REQUIRE_LOCK) {
                require.invoke(modNs);
            }

            Var startFn = (Var) Clojure.var("cn.li.forge1201.mod", "start-forge-mod!");
            if (!startFn.isBound()) {
                throw new IllegalStateException("Clojure bootstrap var is unbound: cn.li.forge1201.mod/start-forge-mod!");
            }
            startFn.invoke();
        } catch (Throwable t) {
            System.err.println("Failed to load Clojure mod implementation:");
            t.printStackTrace();
            throw new RuntimeException("Clojure mod initialization failed — terminating Minecraft startup", t);
        }
    }
}

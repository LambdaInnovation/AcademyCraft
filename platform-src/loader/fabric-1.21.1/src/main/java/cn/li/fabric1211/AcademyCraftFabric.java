package cn.li.fabric1211;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import cn.li.fabric1211.entity.FabricEntities;
import cn.li.mcbase.clj.ClojureInterop;
import cn.li.fabric1211.entity.FabricScriptedEntityAccess;
import net.fabricmc.api.ModInitializer;

/**
 * Fabric mod entry point - bridges to Clojure implementation.
 */
public class AcademyCraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Register entity types first
        FabricEntities.registerEntities();

        // Install shared accessor
        FabricScriptedEntityAccess.install();

        // Initialize Clojure module
        ClojureInterop.requireNamespace("cn.li.fabric1211.mod");

        IFn startFabricMod = Clojure.var("cn.li.fabric1211.mod", "start-fabric-mod!");
        startFabricMod.invoke();
    }
}

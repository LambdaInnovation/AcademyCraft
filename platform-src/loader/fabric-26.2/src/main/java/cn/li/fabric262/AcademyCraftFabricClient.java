package cn.li.fabric262;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import cn.li.fabric262.client.FabricClientRenderSetup;
import cn.li.mcbase.clj.ClojureInterop;
import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric client entry point - bridges to Clojure client initialization.
 */
public class AcademyCraftFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricClientRenderSetup.registerClientHooks();
        FabricClientRenderSetup.registerParticleProviders();
        try {
            ClojureInterop.requireNamespace("cn.li.fabric262.gui.init");
            IFn guiClientInit = Clojure.var("cn.li.fabric262.gui.init", "init-client!");
            guiClientInit.invoke();

            ClojureInterop.requireNamespace("cn.li.fabric262.client.init");
            IFn clientInit = Clojure.var("cn.li.fabric262.client.init", "init-client");
            clientInit.invoke();
        } catch (Throwable t) {
            System.err.println("Failed to run Fabric client initialization (Clojure):");
            t.printStackTrace();
        }
    }
}

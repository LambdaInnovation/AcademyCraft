package cn.li.neoforgebase.bridge;

import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;

/**
 * Persist a loaded {@link ModConfig} without reflective Clojure interop.
 */
public final class ConfigInterop {
    private ConfigInterop() {
    }

    public static void save(ModConfig config) {
        if (config == null) {
            return;
        }
        IConfigSpec.ILoadedConfig loaded = config.getLoadedConfig();
        if (loaded != null) {
            loaded.save();
        }
    }
}

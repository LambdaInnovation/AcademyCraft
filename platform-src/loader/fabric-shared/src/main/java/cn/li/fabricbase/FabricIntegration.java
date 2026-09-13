package cn.li.fabricbase;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Loader-wide Fabric integration helpers. This class deliberately contains no
 * Minecraft-version types; version adapters own all Minecraft API calls.
 */
public final class FabricIntegration {
    private FabricIntegration() {}

    public static boolean isModLoaded(String modId) {
        return modId != null && FabricLoader.getInstance().isModLoaded(modId);
    }
}

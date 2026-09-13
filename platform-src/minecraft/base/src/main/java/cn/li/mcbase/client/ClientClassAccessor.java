package cn.li.mcbase.client;

import net.minecraft.client.player.LocalPlayer;

/**
 * Shared client-side class accessor used by loader-specific client bridges.
 *
 * <p>Keeps class literals out of duplicated Forge/Fabric bridge implementations
 * while still allowing platform layers to own the client-only environment
 * annotations.</p>
 */
public final class ClientClassAccessor {
    private ClientClassAccessor() {}

    public static Class<?> getLocalPlayerClass() {
        return LocalPlayer.class;
    }
}

package cn.li.neoforgebase.bridge;

import cn.li.mcbase.client.ClientClassAccessor;

/**
 * Client-only bridge for accessing client-side MC classes from Clojure AOT code
 * without string-based Class.forName calls.
 *
 * Always call from Clojure through a client-side check (side/client-side?).
 */
public final class ClientPlatformBridge {
    private ClientPlatformBridge() {}

    public static Class<?> getLocalPlayerClass() {
        return ClientClassAccessor.getLocalPlayerClass();
    }
}

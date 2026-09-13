package cn.li.acapi.energy.handle;

import java.util.Objects;

/**
 * Opaque reference to a wireless network model owned by runtime modules.
 */
public final class WirelessNetworkRef {
    private final Object rawNetwork;

    private WirelessNetworkRef(Object rawNetwork) {
        this.rawNetwork = Objects.requireNonNull(rawNetwork, "rawNetwork");
    }

    public static WirelessNetworkRef of(Object rawNetwork) {
        return new WirelessNetworkRef(rawNetwork);
    }

    public Object rawNetwork() {
        return rawNetwork;
    }
}

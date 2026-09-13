package cn.li.acapi.wireless;

/**
 * Canonical key strings for wireless capability registration and lookup.
 *
 * <p>These keys are used by platform-neutral Clojure code when calling
 * platform capability adapters and by Forge registry wiring when binding keys
 * to named capability constants.
 */
public final class WirelessCapabilityKeys {

    public static final String MATRIX = "wireless-matrix";
    public static final String NODE = "wireless-node";
    public static final String GENERATOR = "wireless-generator";
    public static final String RECEIVER = "wireless-receiver";

    private WirelessCapabilityKeys() {}
}
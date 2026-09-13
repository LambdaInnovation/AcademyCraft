package cn.li.acapi.wireless;

/**
 * Platform-neutral interface for wireless matrix blocks.
 * Implemented by Clojure deftype in ac (WirelessMatrixImpl).
 * Exposed as a Forge Capability via CapabilitySlots.
 */
public interface IWirelessMatrix extends IWirelessTile {
    /** How many nodes this matrix can hold. */
    int getMatrixCapacity();

    /** How much energy allowed to balance between nodes each tick. */
    double getMatrixBandwidth();

    /** The max range that this matrix can reach (in blocks). */
    double getMatrixRange();

    /** Network SSID. */
    String getSsid();

    /** Network password. */
    String getPassword();

    /** Name of player who placed this matrix. */
    String getPlacerName();
}

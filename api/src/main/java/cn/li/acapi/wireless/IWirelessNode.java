package cn.li.acapi.wireless;

/**
 * Platform-neutral interface for wireless node blocks.
 * Implemented by Clojure deftype in ac (WirelessNodeImpl).
 * Exposed as a Forge Capability via CapabilitySlots.
 */
public interface IWirelessNode extends IWirelessTile {
    /** Current energy stored. */
    double getEnergy();

    /** Set current energy stored. */
    void setEnergy(double energy);

    /** Maximum energy capacity. */
    double getMaxEnergy();

    /** Energy transfer rate per tick. */
    double getBandwidth();

    /** Maximum number of generators/receivers this node can connect to. */
    int getCapacity();

    /** Signal range in blocks. */
    double getRange();

    /** User-defined node name. */
    String getNodeName();

    /** Network password. */
    String getPassword();

    /** Block position (platform-neutral IBlockPos). */
    Object getBlockPos();
}

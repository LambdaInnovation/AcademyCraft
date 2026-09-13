package cn.li.acapi.wireless;

/**
 * Platform-neutral interface for wireless receiver blocks.
 */
public interface IWirelessReceiver extends IWirelessUser {
    /** How much energy this receiver needs. */
    double getRequiredEnergy();

    /**
     * Inject energy into the machine.
     * @param amt amount to inject (always positive)
     * @return energy not injected (leftover)
     */
    double injectEnergy(double amt);

    /**
     * Pull energy out of the machine.
     * @param amt amount to pull (always positive)
     * @return energy actually pulled
     */
    double pullEnergy(double amt);

    /** Maximum energy this receiver can retrieve each tick. */
    double getReceiverBandwidth();
}

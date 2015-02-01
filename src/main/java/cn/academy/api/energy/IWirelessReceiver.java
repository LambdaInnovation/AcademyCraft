/**
 * 
 */
package cn.academy.api.energy;

/**
 * Energy receiver
 * @author WeathFolD
 */
public interface IWirelessReceiver extends IWirelessTile {
	
	/**
	 * Get how much energy this receiver wanted this tick.
	 */
	double getEnergyRequired();
	
	double getLatency();
	
	/**
	 * Inject the amt energy into the receiver.
	 * @return how many energy not injected(leftover)
	 */
	double injectEnergy(double amt);
}

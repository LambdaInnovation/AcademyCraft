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
	int getEnergyRequired();
	
	/**
	 * Inject the amt energy into the receiver.
	 * @return how many energy not injected(leftover)
	 */
	int injectEnergy(int amt);
}

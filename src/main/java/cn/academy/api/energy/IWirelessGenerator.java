/**
 * 
 */
package cn.academy.api.energy;

/**
 * @author WeathFolD
 */
public interface IWirelessGenerator extends IWirelessTile {
	
	/**
	 * Get the energy output current tick
	 * @param req The energy required
	 * @return The energy generated
	 */
	double getOutput(double req);
	
}

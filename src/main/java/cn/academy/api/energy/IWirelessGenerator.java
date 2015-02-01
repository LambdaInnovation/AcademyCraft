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
	 * @param required the energy required
	 * @return The energy generated, which should never be larger than required
	 */
	int getOutput(int required);
	
}

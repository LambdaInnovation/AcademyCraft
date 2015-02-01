/**
 * 
 */
package cn.academy.api.energy;

/**
 * Indicates single buffer node of a network
 * @author WeathFolD
 */
public interface IWirelessNode extends IWirelessTile {
	
	/**
	 * Set the energy of the node.
	 * @param value the value to change of the energy
	 */
	void setEnergy(double value);
	
	/**
	 * Get the max possible energy of the node.
	 */
	double getMaxEnergy();
	
	/**
	 * Get the current energy stored inside the node.
	 */
	double getEnergy();
	
	/**
	 * Get the maximum permitted abs energy change per tick.
	 */
	double getLatency();
	
	/**
	 * Get the maxium distance that this node can reach.
	 * @return
	 */
	double getTransDistance();
}

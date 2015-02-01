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
	 * Change the energy of the node. It is guarranteed that 
	 * the change value passed in will not downflow nor overflow the tile.
	 * @param delta the value to change for the energy
	 */
	void changeEnergy(int delta);
	
	/**
	 * Get the max possible energy of the node.
	 */
	int getMaxEnergy();
	
	/**
	 * Get the current energy stored inside the node.
	 */
	int getEnergy();
	
	/**
	 * Get the maximum permitted abs energy change per tick.
	 */
	int getMaxLatency();
	
	/**
	 * Get the maxium distance that this node can reach.
	 * @return
	 */
	double getTransDistance();
}

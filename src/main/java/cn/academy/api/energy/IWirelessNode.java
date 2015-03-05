/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
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

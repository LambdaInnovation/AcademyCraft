/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.api.block;

/**
 * Information providing interface of a wireless node.
 * @author WeathFolD
 */
public interface IWirelessNode extends IWirelessTile {
    
    double getMaxEnergy();
    double getEnergy();
    void setEnergy(double value);
    
    /**
     * @return How many energy that this node can transfer each tick.
     */
    double getBandwidth();
    
    int getCapacity();
    
    /**
     * @return How far this node's signal can reach.
     */
    double getRange();
    
    /**
     * @return the user custom name of the node
     */
    String getNodeName();
    
}

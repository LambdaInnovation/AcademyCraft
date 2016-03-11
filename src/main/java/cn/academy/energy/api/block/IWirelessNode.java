/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
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

    /**
     * @return the password of the node
     */
    String getPassword();
    
}

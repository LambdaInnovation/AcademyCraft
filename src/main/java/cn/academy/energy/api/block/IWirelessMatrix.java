/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.api.block;

/**
 * Information providing interface of a wireless matrix.
 * @author WeathFolD
 */
public interface IWirelessMatrix extends IWirelessTile {
    
    /**
     * @return How many nodes it can hold
     */
    int getCapacity();
    
    /**
     * @return How much energy allowed to balance between nodes each tick
     */
    double getBandwidth();
    
    /**
     * @return the max range that this matrix can reach
     */
    double getRange();
    
}

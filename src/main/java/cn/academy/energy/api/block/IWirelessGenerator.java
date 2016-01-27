/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.api.block;

/**
 * @author WeathFolD
 */
public interface IWirelessGenerator extends IWirelessUser {
    
    /**
     * @param req How much energy is required
     * @return How much energy this generator can provide. Must be guaranteed 0<=ret<=req
     */
    public double getProvidedEnergy(double req);
    
    /**
     * @return Max energy transmitted each tick
     */
    public double getBandwidth();
    
}

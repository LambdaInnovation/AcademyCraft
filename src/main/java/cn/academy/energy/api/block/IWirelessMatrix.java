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
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
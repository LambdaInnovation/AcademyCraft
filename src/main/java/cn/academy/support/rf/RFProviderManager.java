/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.support.rf;

import cn.academy.support.EnergyBlockHelper.IEnergyBlockManager;
import cofh.api.energy.IEnergyProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * @author WeAthFolD
 */
public class RFProviderManager implements IEnergyBlockManager {
    
    static final ForgeDirection dir = ForgeDirection.UP;

    @Override
    public boolean isSupported(TileEntity tile) {
        return asProvider(tile) != null;
    }
    
    private IEnergyProvider asProvider(TileEntity te) {
        return te instanceof IEnergyProvider ? (IEnergyProvider) te : null;
    }

    @Override
    public double getEnergy(TileEntity tile) {
        IEnergyProvider provider = asProvider(tile);
        return provider.getEnergyStored(dir) * RFSupport.CONV_RATE;
    }

    @Override
    public void setEnergy(TileEntity tile, double energy) {
        // NOT SUPPORTED
    }

    @Override
    public double charge(TileEntity tile, double amt, boolean ignoreBandwidth) {
        // NOT SUPPORTED
        return amt;
    }

    @Override
    public double pull(TileEntity tile, double amt, boolean ignoreBandwidth) {
        IEnergyProvider provider = asProvider(tile);
        return provider == null ? 0 : RFSupport.CONV_RATE * provider.extractEnergy(dir, (int) (amt / RFSupport.CONV_RATE), false);
    }

}

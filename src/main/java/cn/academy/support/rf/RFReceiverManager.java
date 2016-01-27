/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.support.rf;

import cn.academy.support.EnergyBlockHelper.IEnergyBlockManager;
import cofh.api.energy.IEnergyReceiver;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * @author WeAthFolD
 */
public class RFReceiverManager implements IEnergyBlockManager {
    
    static final ForgeDirection DEFAULT_DIR = ForgeDirection.UP;

    @Override
    public boolean isSupported(TileEntity tile) {
        return asReceiver(tile) != null;
    }
    
    private IEnergyReceiver asReceiver(TileEntity tile) {
        return tile instanceof IEnergyReceiver ? (IEnergyReceiver) tile : null;
    }
    
    @Override
    public double getEnergy(TileEntity tile) {
        IEnergyReceiver rec = asReceiver(tile);
        return rec == null ? 0 : rec.getEnergyStored(DEFAULT_DIR) * RFSupport.CONV_RATE;
    }

    @Override
    public void setEnergy(TileEntity tile, double energy) {
        // NOT SUPPORTED
    }

    @Override
    public double charge(TileEntity tile, double amt, boolean ignoreBandwidth) {
        IEnergyReceiver rec = asReceiver(tile);
        return rec == null ? 0 : amt - rec.receiveEnergy(DEFAULT_DIR, (int) (amt / RFSupport.CONV_RATE), false) * RFSupport.CONV_RATE;
    }

    @Override
    public double pull(TileEntity tile, double amt, boolean ignoreBandwidth) {
        // NOT SUPPORTED
        return 0;
    }

}

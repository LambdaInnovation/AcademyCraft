/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.support.ic2;

import cn.academy.support.EnergyBlockHelper.IEnergyBlockManager;
import ic2.api.energy.tile.IEnergySink;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * @author WeAthFolD
 */
public class EUSinkManager implements IEnergyBlockManager {

    @Override
    public boolean isSupported(TileEntity tile) {
        return asSink(tile) != null;
    }

    IEnergySink asSink(TileEntity tile) {
        return tile instanceof IEnergySink ? (IEnergySink) tile : null;
    }

    @Override
    public double getEnergy(TileEntity tile) {
        // NOT SUPPORTED
        return 0;
    }

    @Override
    public void setEnergy(TileEntity tile, double energy) {
        // NOT SUPPORTED
        return;
    }

    @Override
    public double charge(TileEntity tile, double amt, boolean ignoreBandwidth) {
        IEnergySink sink = asSink(tile);
        if(sink != null) {
            return IC2Support.CONV_RATE * sink.injectEnergy(ForgeDirection.UP, amt / IC2Support.CONV_RATE, ignoreBandwidth ? 233333 : 1024);
        }
        return amt;
    }

    @Override
    public double pull(TileEntity tile, double amt, boolean ignoreBandwidth) {
        // NOT SUPPORTED
        return 0;
    }

}

/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.support.rf;

import cn.academy.core.block.TileGeneratorBase;
import cn.lambdalib2.annoreg.core.RegWithName;
import cn.lambdalib2.annoreg.core.Registrant;
import cn.lambdalib2.annoreg.mc.RegTileEntity;
import cofh.api.energy.IEnergyReceiver;
import net.minecraftforge.common.util.ForgeDirection;

import static cn.academy.support.rf.RFSupport.if2rf;
import static cn.academy.support.rf.RFSupport.rf2if;

@Registrant
@RegTileEntity
@RegWithName("rf_input")
public class TileRFInput extends TileGeneratorBase implements IEnergyReceiver {
    
    public TileRFInput() {
        super("ac_rf_input", 0, 2000, 100);
    }

    @Override
    public boolean canConnectEnergy(ForgeDirection from) {
        return true;
    }

    @Override
    public int receiveEnergy(ForgeDirection from, int maxReceive,
            boolean simulate) {
        int amount = (int) rf2if(maxReceive);
        return (int) (maxReceive - if2rf(addEnergy(amount, simulate)));
    }

    @Override
    public int getEnergyStored(ForgeDirection from) {
        return if2rf(getEnergy());
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection from) {
        return (int) if2rf(2000);
    }

    @Override
    public double getGeneration(double required) {
        return 0;
    }
    
}

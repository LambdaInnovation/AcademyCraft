package cn.academy.support.rf;

import cn.academy.block.tileentity.TileGeneratorBase;
import cn.lambdalib2.registry.mc.RegTileEntity;
import cofh.redstoneflux.api.IEnergyReceiver;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.Optional;

import static cn.academy.support.rf.RFSupport.if2rf;
import static cn.academy.support.rf.RFSupport.rf2if;

@RegTileEntity
@Optional.Interface(modid = "redstoneflux", iface = "cofh.redstoneflux.api.IEnergyReceiver")
public class TileRFInput extends TileGeneratorBase implements IEnergyReceiver
{
    
    public TileRFInput() {
        super("ac_rf_input", 0, 2000, 100);
    }

    @Override
    public boolean canConnectEnergy(EnumFacing from) {
        return true;
    }

    @Override
    public int receiveEnergy(EnumFacing from, int maxReceive,
            boolean simulate) {
        int amount = (int) rf2if(maxReceive);
        return (int) (maxReceive - if2rf(addEnergy(amount, simulate)));
    }

    @Override
    public int getEnergyStored(EnumFacing from) {
        return if2rf(getEnergy());
    }

    @Override
    public int getMaxEnergyStored(EnumFacing from) {
        return (int) if2rf(2000);
    }

    @Override
    public double getGeneration(double required) {
        return 0;
    }
    
}
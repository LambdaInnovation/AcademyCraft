package cn.academy.support.rf;

import cn.academy.support.EnergyBlockHelper.IEnergyBlockManager;
import cofh.redstoneflux.api.IEnergyReceiver;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

/**
 * @author WeAthFolD
 */
public class RFReceiverManager implements IEnergyBlockManager {
    
    static final EnumFacing DEFAULT_DIR = EnumFacing.UP;

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
        return rec == null ? amt : amt - rec.receiveEnergy(DEFAULT_DIR, (int) amt, false);
    }

    @Override
    public double pull(TileEntity tile, double amt, boolean ignoreBandwidth) {
        // NOT SUPPORTED
        return 0;
    }

}
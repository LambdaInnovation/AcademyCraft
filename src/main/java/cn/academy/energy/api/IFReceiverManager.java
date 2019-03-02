package cn.academy.energy.api;

import cn.academy.energy.api.block.IWirelessReceiver;
import cn.academy.support.EnergyBlockHelper;
import cn.academy.support.EnergyBlockHelper.IEnergyBlockManager;
import cn.lambdalib2.registry.StateEventCallback;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * @author WeAthFolD
 */
public class IFReceiverManager implements IEnergyBlockManager {

    public static IFReceiverManager instance = new IFReceiverManager();

    @StateEventCallback
    private static void init(FMLInitializationEvent ev) {
        EnergyBlockHelper.register(instance);
    }

    private IFReceiverManager() {
    }

    @Override
    public boolean isSupported(TileEntity tile) {
        return tile instanceof IWirelessReceiver;
    }

    @Override
    public double getEnergy(TileEntity tile) {
        return 0;
    }

    @Override
    public void setEnergy(TileEntity tile, double energy) {
        // throw new UnsupportedOperationException();
    }

    @Override
    public double charge(TileEntity tile, double amt, boolean ignoreBandwidth) {
        return ((IWirelessReceiver) tile).injectEnergy(amt);
    }

    @Override
    public double pull(TileEntity tile, double amt, boolean ignoreBandwidth) {
        return ((IWirelessReceiver) tile).pullEnergy(amt);
    }

}
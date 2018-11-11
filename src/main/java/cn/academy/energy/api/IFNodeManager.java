package cn.academy.energy.api;

import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.support.EnergyBlockHelper;
import cn.academy.support.EnergyBlockHelper.IEnergyBlockManager;
import cn.lambdalib2.registry.StateEventCallback;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * @author WeAthFolD
 */

public class IFNodeManager implements IEnergyBlockManager {

    public static IFNodeManager instance = new IFNodeManager();

    @StateEventCallback
    private static void init(FMLInitializationEvent ev) {
        EnergyBlockHelper.register(instance);
    }

    @Override
    public boolean isSupported(TileEntity tile) {
        return tile instanceof IWirelessNode;
    }

    @Override
    public double getEnergy(TileEntity tile) {
        return ((IWirelessNode) tile).getEnergy();
    }

    @Override
    public void setEnergy(TileEntity tile, double energy) {
        ((IWirelessNode) tile).setEnergy(energy);
    }

    @Override
    public double charge(TileEntity tile, double amt, boolean ignoreBandwidth) {
        IWirelessNode node = ((IWirelessNode) tile);
        double max = node.getMaxEnergy() - node.getEnergy();
        double chg = Math.min(amt, max);
        if (!ignoreBandwidth)
            chg = Math.min(node.getBandwidth(), chg);

        node.setEnergy(node.getEnergy() + chg);
        return amt - chg;
    }

    @Override
    public double pull(TileEntity tile, double amt, boolean ignoreBandwidth) {
        IWirelessNode node = (IWirelessNode) tile;
        double max = node.getEnergy();
        double pull = Math.min(max, amt);
        if (!ignoreBandwidth)
            pull = Math.min(node.getBandwidth(), pull);

        node.setEnergy(node.getEnergy() - pull);
        return pull;
    }

}
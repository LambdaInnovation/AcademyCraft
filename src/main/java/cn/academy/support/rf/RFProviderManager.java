/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
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

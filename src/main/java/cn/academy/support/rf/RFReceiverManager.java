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

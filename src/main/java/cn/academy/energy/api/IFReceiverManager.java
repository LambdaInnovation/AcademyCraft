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
package cn.academy.energy.api;

import net.minecraft.tileentity.TileEntity;
import cn.academy.energy.api.block.IWirelessReceiver;
import cn.academy.support.EnergyBlockHelper;
import cn.academy.support.EnergyBlockHelper.IEnergyBlockManager;
import cn.annoreg.core.Registrant;

/**
 * @author WeAthFolD
 */
@Registrant
public class IFReceiverManager implements IEnergyBlockManager {
	
	public static IFReceiverManager instance = new IFReceiverManager();
	
	private IFReceiverManager() {
		EnergyBlockHelper.register(this);
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

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

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
import ic2.api.energy.tile.IEnergySource;
import net.minecraft.tileentity.TileEntity;

/**
 * @author WeAthFolD
 */
public class EUSourceManager implements IEnergyBlockManager {

	@Override
	public boolean isSupported(TileEntity tile) {
		return asSource(tile) != null;
	}

	IEnergySource asSource(TileEntity tile) {
		return tile instanceof IEnergySource ? (IEnergySource) tile : null;
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
		// NOT SUPPORTED
		return amt;
	}

	@Override
	public double pull(TileEntity tile, double amt, boolean ignoreBandwidth) {
		IEnergySource src = asSource(tile);
		if(src != null) {
			double todraw = Math.min(src.getOfferedEnergy(), amt / IC2Support.CONV_RATE);
			src.drawEnergy(todraw);
			return todraw * IC2Support.CONV_RATE;
		}
		return 0;
	}

}

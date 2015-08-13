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
package cn.academy.support.te;

import net.minecraftforge.common.util.ForgeDirection;
import cofh.api.energy.IEnergyReceiver;
import cn.academy.core.block.TileGeneratorBase;

public class TileRFInput extends TileGeneratorBase implements IEnergyReceiver {

	/** The convert rate (RF * RATE = IF) */
	private static final float RATE = 1f; 
	
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
		int amount = (int) (maxReceive * RATE);
		return (int) (maxReceive - addEnergy(amount) / RATE);
	}

	@Override
	public int getEnergyStored(ForgeDirection from) {
		return (int) ((int) getEnergy() / RATE);
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from) {
		return (int) (2000 / RATE);
	}

	@Override
	public double getGeneration(double required) {
		return 0;
	}
	
}

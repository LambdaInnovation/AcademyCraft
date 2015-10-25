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

import net.minecraftforge.common.util.ForgeDirection;
import cofh.api.energy.IEnergyProvider;
import cn.academy.core.block.TileReceiverBase;
import cn.annoreg.core.RegWithName;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegTileEntity;

@Registrant
@RegTileEntity
@RegWithName("rf_output")
public class TileRFOutput extends TileReceiverBase implements IEnergyProvider {
	
	/** The convert rate (RF * RATE = IF) */
	private static final float RATE = RFSupport.CONV_RATE;

	public TileRFOutput() {
		super("ac_rf_output", 0, 2000, 100);
	}

	@Override
	public boolean canConnectEnergy(ForgeDirection from) {
		return true;
	}

	@Override
	public int extractEnergy(ForgeDirection from, int maxExtract,
			boolean simulate) {
		int e = (int) energy;
		if(!simulate) {
			energy -= maxExtract * RATE;
			if(energy < 0d) energy = 0d;
		}
		return (int) Math.min(e / RATE, maxExtract);
	}

	@Override
	public int getEnergyStored(ForgeDirection from) {
		return (int) (energy / RATE);
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from) {
		return (int) (2000 / RATE);
	}

}

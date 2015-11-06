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

import cn.academy.core.block.TileGeneratorBase;
import cn.lambdalib.annoreg.core.RegWithName;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegTileEntity;
import cofh.api.energy.IEnergyReceiver;
import net.minecraftforge.common.util.ForgeDirection;

import static cn.academy.support.rf.RFSupport.if2rf;
import static cn.academy.support.rf.RFSupport.rf2if;

@Registrant
@RegTileEntity
@RegWithName("rf_input")
public class TileRFInput extends TileGeneratorBase implements IEnergyReceiver {
	
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
		int amount = (int) rf2if(maxReceive);
		return (int) (maxReceive - if2rf(addEnergy(amount, simulate)));
	}

	@Override
	public int getEnergyStored(ForgeDirection from) {
		return if2rf(getEnergy());
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from) {
		return (int) if2rf(2000);
	}

	@Override
	public double getGeneration(double required) {
		return 0;
	}
	
}

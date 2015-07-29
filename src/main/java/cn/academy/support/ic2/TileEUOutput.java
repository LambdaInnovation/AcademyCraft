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

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import ic2.api.energy.tile.IEnergySource;
import cn.academy.core.block.TileReceiverBase;

/**
 * 
 * @author KSkun
 */
public class TileEUOutput extends TileReceiverBase implements IEnergySource {
	
	double buffer = 0d;

	public TileEUOutput() {
		super("ac_eu_output", 0, 0, 100);
	}

	@Override
	public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction) {
		return true;
	}

	@Override
	public double getOfferedEnergy() {
		return buffer;
	}

	@Override
	public void drawEnergy(double amount) {}

	@Override
	public int getSourceTier() {
		return 2;
	}
	
	@Override
	public double injectEnergy(double amount) {
		buffer = amount;
		return 0;
	}

}

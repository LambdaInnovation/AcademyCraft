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
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.support.EnergyBlockHelper;
import cn.academy.support.EnergyBlockHelper.IEnergyBlockManager;
import cn.annoreg.core.Registrant;

/**
 * @author WeAthFolD
 */
@Registrant
public class IFNodeManager implements IEnergyBlockManager {
	
	public static IFNodeManager instance = new IFNodeManager();
	
	private IFNodeManager() {
		EnergyBlockHelper.register(this);
	}

	@Override
	public boolean isSupported(TileEntity tile) {
		return tile instanceof IWirelessNode;
	}

	@Override
	public double getEnergy(TileEntity tile) {
		return ((IWirelessNode)tile).getEnergy();
	}

	@Override
	public void setEnergy(TileEntity tile, double energy) {
		((IWirelessNode)tile).setEnergy(energy);
	}

	@Override
	public double charge(TileEntity tile, double amt, boolean ignoreBandwidth) {
		IWirelessNode node = ((IWirelessNode)tile);
		double max = node.getMaxEnergy() - node.getEnergy();
		double chg = Math.min(amt, max);
		if(!ignoreBandwidth)
			chg = Math.min(node.getBandwidth(), chg);
		
		node.setEnergy(node.getEnergy() + chg);
		return amt - chg;
	}

	@Override
	public double pull(TileEntity tile, double amt, boolean ignoreBandwidth) {
		IWirelessNode node = (IWirelessNode) tile;
		double max = node.getEnergy();
		double pull = Math.min(max, amt);
		if(!ignoreBandwidth)
			pull = Math.min(node.getBandwidth(), pull);
		
		node.setEnergy(node.getEnergy() - pull);
		return pull;
	}

}

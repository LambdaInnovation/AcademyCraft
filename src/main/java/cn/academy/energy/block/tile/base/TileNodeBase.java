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
package cn.academy.energy.block.tile.base;

import net.minecraft.nbt.NBTTagCompound;
import cn.academy.api.energy.IWirelessNode;
import cn.academy.core.AcademyCraft;
import cn.academy.core.energy.WirelessSystem;
import cn.academy.energy.msg.MsgEnergyHeartbeat;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
public abstract class TileNodeBase extends TileWirelessBase implements IWirelessNode {

	protected final double maxEnergy, latency, transDist;
	protected double energy;
	
	@SideOnly(Side.CLIENT)
	public boolean isLoaded; //Client fake load flag, used for rendering.
	
	public TileNodeBase(double _maxEnergy, double _latency, double _dist) {
		maxEnergy = _maxEnergy;
		latency = _latency;
		transDist = _dist;
	}
	
	@Override
	protected void onSync() {
		if(!worldObj.isRemote) {
			AcademyCraft.netHandler.sendToAllAround(new MsgEnergyHeartbeat(this), new TargetPoint(worldObj.provider.dimensionId, xCoord + .5, yCoord + .5, zCoord + .5, 6));
		}
	}
	
	public boolean activate(String channel) {
		if(worldObj.isRemote || this.isConnected()) {
			return false;
		}
		WirelessSystem.registerNode(this, channel);
		return true;
	}

	@Override
	public void setEnergy(double value) {
		energy = Math.max(0, Math.min(maxEnergy, value));
	}

	@Override
	public double getMaxEnergy() {
		return maxEnergy;
	}
	
	@Override
	public double getEnergy() {
		return energy;
	}

	@Override
	public double getLatency() {
		return latency;
	}

	@Override
	public double getTransDistance() {
		return transDist;
	}
	
    @Override
	public void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);
        energy = tag.getDouble("energy");
    }

    @Override
	public void writeToNBT(NBTTagCompound tag)
    {
        super.writeToNBT(tag);
        tag.setDouble("energy", energy);
    }

}

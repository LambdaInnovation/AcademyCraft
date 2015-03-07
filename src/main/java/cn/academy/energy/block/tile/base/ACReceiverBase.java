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
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySink;
import cn.academy.api.energy.IWirelessReceiver;

/**
 * @author WeathFolD
 *
 */
public abstract class ACReceiverBase extends TileUserBase implements 
	IWirelessReceiver, IEnergySink {

	public int maxInput = 512;
	protected double maxEnergy;
	
	public double curEnergy;
	
	protected boolean init = false;
	
	public ACReceiverBase() {}
	
	public void setMaxEnergy(double d) {
		maxEnergy = d;
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		if(!init) {
			init = true;
			init();
		}
	}
	
	@Override
	protected void onUnload() { 
		super.onUnload();
		if(!worldObj.isRemote)
			MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
	}
	
	@Override
	protected void init() {
		super.init();
		if(!worldObj.isRemote) {
			MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
		}
	}
	
	public double getMaxEnergy() {
		return maxEnergy;
	}

	@Override
	public boolean acceptsEnergyFrom(TileEntity emitter,
			ForgeDirection direction) {
		return true;
	}

	@Override
	public double demandedEnergyUnits() {
		return Math.max(0, getMaxEnergy() - curEnergy);
	}

	@Override
	public double injectEnergyUnits(ForgeDirection fd, double amt) {
		curEnergy += amt;
		double left = Math.max(0, curEnergy - getMaxEnergy());
		curEnergy -= left;
		return left;
	}

	@Override
	public int getMaxSafeInput() {
		return maxInput;
	}
	
    @Override
	public void readFromNBT(NBTTagCompound nbt) {
    	super.readFromNBT(nbt);
    	curEnergy = nbt.getDouble("energy");
    }
    
    @Override
	public void writeToNBT(NBTTagCompound nbt) {
    	super.writeToNBT(nbt);
    	nbt.setDouble("energy", curEnergy);
    }

	@Override
	public double getEnergyRequired() {
		return Math.min(maxInput, maxEnergy - curEnergy);
	}

	@Override
	public double getLatency() {
		return maxInput;
	}

	@Override
	public double injectEnergy(double amt) {
		double req = maxEnergy - curEnergy;
		double real = Math.min(req, amt);
		curEnergy += real;
		return amt - real;
	}

}

/**
 * 
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
	
	protected void onUnload() { 
		super.onUnload();
		if(!worldObj.isRemote)
			MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
	}
	
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
	
    public void readFromNBT(NBTTagCompound nbt) {
    	super.readFromNBT(nbt);
    	curEnergy = nbt.getDouble("energy");
    }
    
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

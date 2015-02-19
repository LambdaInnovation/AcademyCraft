/**
 * 
 */
package cn.academy.energy.block.tile.base;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySource;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import cn.academy.api.energy.IWirelessGenerator;

/**
 * @author WeathFolD
 *
 */
public class ACGeneratorBase extends TileUserBase implements IWirelessGenerator, IEnergySource {
	
	protected double energy;
	protected double maxTransmit = 128;
	protected double maxEnergy;

	public ACGeneratorBase() {}
	
	protected void setMaxEnergy(double d) {
		maxEnergy = d;
	}
	
	@Override
	public void init() {
		if(!getWorldObj().isRemote) {
			MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
		}
		super.onUnload();
	}
	
	@Override
	protected void onUnload() {
		if(!getWorldObj().isRemote) {
			MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
		}
		super.onUnload();
	}

	@Override
	public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction) {
		return true;
	}

	@Override
	public double getSearchRange() {
		return 32;
	}
	
    public void addEnergy(double toAdd) {
    	double req = maxEnergy - energy;
    	double real = Math.min(req, toAdd);
    	energy += real;
    }

	@Override
	public double getOutput(double req) {
		double csm = Math.min(req, Math.min(128, energy));
		energy -= csm;
		return csm;
	}

	@Override
	public double getOfferedEnergy() {
		return Math.min(maxTransmit, energy);
	}

	@Override
	public void drawEnergy(double amount) {
		energy -= amount;
		if(energy < 0.0) energy = 0.0;
	}
	
    public void readFromNBT(NBTTagCompound nbt) {
    	super.readFromNBT(nbt);
    	energy = nbt.getDouble("energy");
    }
    
    public void writeToNBT(NBTTagCompound nbt) {
    	super.writeToNBT(nbt);
    	nbt.setDouble("energy", energy);
    }

}

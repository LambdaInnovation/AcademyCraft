/**
 * 
 */
package cn.academy.energy.block.tile.base;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraft.nbt.NBTTagCompound;
import cn.academy.api.energy.IWirelessNode;
import cn.academy.core.AcademyCraft;
import cn.academy.core.energy.WirelessSystem;
import cn.academy.energy.msg.MsgEnergyHeartbeat;

/**
 * @author WeathFolD
 *
 */
public abstract class TileNodeBase extends TileWirelessBase implements IWirelessNode {

	protected final double maxEnergy, latency, transDist;
	protected double energy;
	
	public TileNodeBase(double _maxEnergy, double _latency, double _dist) {
		maxEnergy = _maxEnergy;
		latency = _latency;
		transDist = _dist;
	}
	
	@Override
	protected void onUpdate() {
		if(!worldObj.isRemote)
			AcademyCraft.netHandler.sendToAllAround(new MsgEnergyHeartbeat(this), new TargetPoint(worldObj.provider.dimensionId, xCoord + .5, yCoord + .5, zCoord + .5, 10));
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

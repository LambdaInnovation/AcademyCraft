/**
 * 
 */
package cn.academy.misc.block.energy.tile.base;

import cn.academy.api.energy.IWirelessNode;
import cn.academy.core.energy.WirelessSystem;
import cn.academy.misc.block.energy.tile.impl.TileWirelessBase;

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
	
	public boolean activate(String channel) {
		if(worldObj.isRemote || this.isConnected()) {
			return false;
		}
		WirelessSystem.registerNode(this, channel);
		return true;
	}

	@Override
	public void setEnergy(double value) {
		energy = value;
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

}

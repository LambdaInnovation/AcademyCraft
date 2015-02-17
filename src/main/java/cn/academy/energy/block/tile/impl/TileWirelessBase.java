/**
 * 
 */
package cn.academy.energy.block.tile.impl;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import cn.academy.api.energy.IWirelessNode;
import cn.academy.api.energy.IWirelessTile;
import cn.academy.core.energy.WirelessSystem;
import cn.liutils.util.space.IBlockFilter;

/**
 * @author WeathFolD
 *
 */
public abstract class TileWirelessBase extends TileEntity implements
		IWirelessTile {
	
	private static final int UPDATE_RATE = 5;
	private int updateTicker;
	
	//Those are just for loading purpose.
	boolean loaded;
	String channelToLoad;

	public TileWirelessBase() {}
	
	@Override
	public void updateEntity() {
		++updateTicker;
		
		
		if(updateTicker >= UPDATE_RATE) {
			updateTicker = 0;
			this.markDirty();
		}
	}
	
    public void onChunkUnload() {
    	onUnload();
    }
    
    public void invalidate() {
    	super.invalidate();
    	onUnload();
    }
    
    public boolean isConnected() {
    	return WirelessSystem.isTileRegistered(this);
    }
    
    public String getChannel() {
    	return WirelessSystem.getTileChannel(this);
    }
    
    protected void onUnload() {
    	if(!worldObj.isRemote) {
    		WirelessSystem.unregisterTile(this);
    	}
    }
	
	//Sandbox methods
	public IWirelessNode getNearestNode() {
		return WirelessSystem.getNearestNode(getWorldObj(), xCoord, yCoord, zCoord);
	}
	
	public List<IWirelessNode> getAvailableNodes(double range, int max) {
		return WirelessSystem.getAvailableNodes(getWorldObj(), xCoord, yCoord, zCoord, range, max);
	}

}

/**
 * 
 */
package cn.academy.misc.block.energy;

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
	
	private boolean connected = false; //If this tile was connected into an wireless net
	protected String channel; //cur channel
	
	private static final int UPDATE_RATE = 5;
	private int updateTicker;

	public TileWirelessBase() {}
	
	@Override
	public void updateEntity() {
		if(!worldObj.isRemote) {
			if(++updateTicker == UPDATE_RATE) {
				updateTicker = 0;
				
				checkValid();
			}
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
    	return connected;
    }
    
    protected void setConnected(String _channel) {
    	channel = _channel;
    	connected = true;
    	checkValid();
    }
    
    protected void onUnload() {
    	if(!worldObj.isRemote) {
    		checkValid();
    		if(connected) {
    			WirelessSystem.unregisterTile(this);
    		}
    	}
    }
    
    protected void checkValid() {
    	if(connected) {
    		if(!WirelessSystem.isTileIn(this, channel))
    			connected = false;
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

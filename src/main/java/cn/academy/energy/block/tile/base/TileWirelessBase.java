/**
 * 
 */
package cn.academy.energy.block.tile.base;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import cn.academy.api.energy.IWirelessNode;
import cn.academy.api.energy.IWirelessTile;
import cn.academy.core.energy.WirelessSystem;
import cn.liutils.util.misc.Pair;
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

	public TileWirelessBase() {}
	
	@Override
	public void updateEntity() {
		++updateTicker;
		if(!loaded) {
			loaded = true;
			init();
		}
		
		if(updateTicker >= UPDATE_RATE) {
			updateTicker = 0;
			onUpdate();
			this.markDirty();
		}
	}
	
	protected void init() {}
	
	protected void onUpdate() {}
	
	public abstract double getSearchRange(); 
	
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
	
	public List<String> getAvailableChannels(int max) {
		return WirelessSystem.getAvailableChannels(getWorldObj(), xCoord, yCoord, zCoord, getSearchRange(), max);
	}
	
	public List<Pair<IWirelessNode, String>> getAvailableNodes(int max) {
		return WirelessSystem.getAvailableNodes(getWorldObj(), xCoord, yCoord, zCoord, getSearchRange(), max);
	}

}

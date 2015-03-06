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

import java.util.List;

import net.minecraft.tileentity.TileEntity;
import cn.academy.api.energy.IWirelessNode;
import cn.academy.api.energy.IWirelessTile;
import cn.academy.core.energy.WirelessSystem;
import cn.liutils.util.misc.Pair;

/**
 * @author WeathFolD
 */
public abstract class TileWirelessBase extends TileEntity implements
		IWirelessTile {
	
	private static final int SYNC_RATE = 20;
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
		
		if(updateTicker >= SYNC_RATE) {
			updateTicker = 0;
			onSync();
			this.markDirty();
		}
	}
	
	protected void init() {}
	
	protected void onSync() {}
	
	public abstract double getSearchRange(); 
	
    @Override
	public void onChunkUnload() {
    	onUnload();
    }
    
    @Override
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

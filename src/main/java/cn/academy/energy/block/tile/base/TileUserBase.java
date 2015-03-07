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
import cn.academy.api.energy.IWirelessNode;
import cn.academy.core.energy.WirelessSystem;

/**
 * @author WeathFolD
 *
 */
public abstract class TileUserBase extends TileWirelessBase {
	
	int MAX_RETRY = 5, RETRY_INTERVAL = 10;
	int[] targNode;
	int retryTicker;

	public TileUserBase() {}

	@Override
	public void updateEntity() {
		super.updateEntity();
		if(targNode != null) {
			if(retryTicker == 0) {
				retryTicker = RETRY_INTERVAL;
			}
			TileEntity te = worldObj.getTileEntity(targNode[0], targNode[1], targNode[2]);
			if(te instanceof IWirelessNode) {
				IWirelessNode node = (IWirelessNode) te;
				if(WirelessSystem.isTileRegistered(node)) {
					WirelessSystem.attachTile(this, node);
					targNode = null;
				}
			}
			--retryTicker;
		}
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		IWirelessNode node;
		if((node = WirelessSystem.getConnectedNode(this)) != null) {
			TileEntity te = (TileEntity) node;
			tag.setIntArray("conn", new int[] { te.xCoord, te.yCoord, te.zCoord });
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		targNode = tag.getIntArray("conn");
		if(targNode.length != 3)
			targNode = null;
	}
}

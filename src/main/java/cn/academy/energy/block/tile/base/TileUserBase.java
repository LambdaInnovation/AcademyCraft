/**
 * 
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

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
public abstract class TileUserBase extends TileWirelessBase {

	public TileUserBase() {}

	public void register(IWirelessNode node) {
		if(this.isConnected()) 
			return;
		WirelessSystem.attachTile(this, node);
	}
}

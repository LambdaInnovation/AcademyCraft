/**
 * 
 */
package cn.academy.core.block;

import cn.liutils.api.util.EntityManipHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

/**
 * 能力开发机的TE
 * @author WeathFolD
 */
public class TileDeveloper extends TileEntity {
	
	private EntityPlayer user;

	public TileDeveloper() { }
	
	/**
	 * 尝试让某个玩家使用开发机
	 * @param player
	 * @return if attempt successful
	 */
	public boolean use(EntityPlayer player) {
		if(user != null) return false;
		user = player;
		EntityManipHandler.addEntityManip(new DevPlayerManip(user, this), true);
		return true;
	}
	
	public void userQuit() {
		user = null;
	}
	
	public EntityPlayer getUser() {
		return user;
	}

}

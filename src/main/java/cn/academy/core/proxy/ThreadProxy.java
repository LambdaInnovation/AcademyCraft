package cn.academy.core.proxy;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.api.ctrl.SyncAction;
import cn.liutils.util.helper.PlayerData;

/**
 * Any side-dependent utility goes here, including basic API functions,
 * skill effects, etc.
 * Implement in {@link CommonThreadProxy} if possible.
 * @author acaly
 *
 */
public interface ThreadProxy {
    
	PlayerData getPlayerData(EntityPlayer player);
	
	/**
	 * Return a string value that is guaranteed to be different between 
	 * sides.
	 * @return
	 */
	String getSideId();
	
	EntityPlayer getThePlayer();
}

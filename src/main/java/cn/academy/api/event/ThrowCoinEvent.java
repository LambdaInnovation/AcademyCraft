/**
 * 
 */
package cn.academy.api.event;

import cn.academy.misc.entity.EntityThrowingCoin;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Fired when a player starts throwing a coin.
 * @author WeathFolD
 */
public class ThrowCoinEvent extends PlayerEvent {

	public final EntityThrowingCoin coin;
	
	public ThrowCoinEvent(EntityPlayer player, EntityThrowingCoin _coin) {
		super(player);
		coin = _coin;
	}

}

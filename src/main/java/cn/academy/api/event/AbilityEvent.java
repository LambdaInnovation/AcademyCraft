/**
 * 
 */
package cn.academy.api.event;

import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Generic Ability-related events.
 * @author WeathFolD
 */
public class AbilityEvent extends PlayerEvent {

	public final AbilityData data;
	
	public AbilityEvent(AbilityData _data) {
		super(_data.getPlayer());
		data = _data;
	}
	
	/**
	 * Fired when player changes his category. At the moment the data is already in the changed state.
	 * Fired only in server side.
	 */
	public static class ChangeCategory extends AbilityEvent {
		public ChangeCategory(AbilityData _data) {
			super(_data);
		}
	}

}

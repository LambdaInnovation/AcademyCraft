/**
 * 
 */
package cn.academy.api.event;

import cpw.mods.fml.common.eventhandler.Event;
import cn.academy.api.data.AbilityData;
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
	 * Fired in both client and server side.
	 */
	public static class ChangeCategory extends AbilityEvent {
		public ChangeCategory(AbilityData _data) {
			super(_data);
		}
	}
	
	/**
	 * Fired when player pressed the key but was unable to perform certain actions.
	 */
	public static class AbortControl extends Event {
		public AbortControl() {}
	}

}

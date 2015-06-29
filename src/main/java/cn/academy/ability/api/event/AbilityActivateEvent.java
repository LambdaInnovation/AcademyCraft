/**
 * 
 */
package cn.academy.ability.api.event;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Fired both CLIENT and SERVER, when player has activated
 * his/her ability using activate key ('V').
 * @author WeAthFolD
 */
public class AbilityActivateEvent extends AbilityEvent {

	public AbilityActivateEvent(EntityPlayer _player) {
		super(_player);
	}

}

package cn.academy.event.ability;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Fired both CLIENT and SERVER, when player has deactivated
 * his/her ability using activate key ('V').
 * @author WeAthFolD
 */
public class AbilityDeactivateEvent extends AbilityEvent {

    public AbilityDeactivateEvent(EntityPlayer _player) {
        super(_player);
    }

}
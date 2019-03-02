package cn.academy.event.ability;

import net.minecraft.entity.player.EntityPlayer;


/**
 * Fired only in client, when the player has just changed his preset using the
 * Preset Switch key(default 'C').
 * @author WeAthFolD
 */
public class PresetSwitchEvent extends AbilityEvent {

    public PresetSwitchEvent(EntityPlayer _player) {
        super(_player);
    }

}
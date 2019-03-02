package cn.academy.event.ability;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Fired BOTH CLIENT AND SERVER. After player has updated his preset.
 * This event is used to indicate that a preset has been UPDATED. It is not necessarily
 * fired only when player edits his preset using UI, but also when data are synced from
 * server/client.
 * @author WeAthFolD
 */
public class PresetUpdateEvent extends AbilityEvent {

    public PresetUpdateEvent(EntityPlayer _player) {
        super(_player);
    }

}
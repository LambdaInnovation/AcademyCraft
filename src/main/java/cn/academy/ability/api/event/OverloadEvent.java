package cn.academy.ability.api.event;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Fired when player is overloaded.
 * Side: Server only
 */
public class OverloadEvent extends AbilityEvent {
    public OverloadEvent(EntityPlayer _player) {
        super(_player);
    }
}

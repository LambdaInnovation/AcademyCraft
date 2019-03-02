package cn.academy.event.ability;

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
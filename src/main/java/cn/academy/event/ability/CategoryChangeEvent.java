package cn.academy.event.ability;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Fired in both sides, when the player category has just changed.
 * @author WeAthFolD
 */
public class CategoryChangeEvent extends AbilityEvent {

    public CategoryChangeEvent(EntityPlayer _player) {
        super(_player);
    }
    
}
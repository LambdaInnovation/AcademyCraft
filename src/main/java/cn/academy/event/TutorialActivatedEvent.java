package cn.academy.event;

import cn.academy.tutorial.ACTutorial;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Fired at both client and server when player has activated a new tutorial.
 */
public class TutorialActivatedEvent extends PlayerEvent {

    public final ACTutorial tutorial;

    public TutorialActivatedEvent(EntityPlayer player, ACTutorial tutorial) {
        super(player);
        this.tutorial = tutorial;
    }

}
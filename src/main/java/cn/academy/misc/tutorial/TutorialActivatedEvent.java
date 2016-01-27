/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.tutorial;

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

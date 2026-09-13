package cn.li.forge1201.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired on both CLIENT and SERVER when a tutorial is newly activated for a player.
 * Mirrors the upstream TutorialActivatedEvent pattern.
 *
 * Posted to {@code MinecraftForge.EVENT_BUS} (Forge game bus) so other mods and the
 * notification system can react to tutorial unlocks.
 */
public class TutorialActivatedEvent extends Event {

    private final Player player;
    private final String tutorialId;

    public TutorialActivatedEvent(Player player, String tutorialId) {
        this.player = player;
        this.tutorialId = tutorialId;
    }

    public Player getPlayer() {
        return player;
    }

    public String getTutorialId() {
        return tutorialId;
    }
}

package cn.li.mcver;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Cross-version advancement grant helpers.
 * 1.20.1: Advancement + getAdvancement.
 */
public final class AdvancementAccess {
    private AdvancementAccess() {
    }

    public static String playerName(ServerPlayer player) {
        return player.getGameProfile().getName();
    }

    /**
     * Award all remaining criteria for {@code advancementId}.
     *
     * @return false when the advancement is missing
     */
    public static boolean grantAllRemaining(ServerPlayer player, String advancementId) {
        MinecraftServer server = McAccess.serverOf(player);
        if (server == null) {
            return false;
        }
        Advancement advancement = server.getAdvancements().getAdvancement(ResourceLocations.parse(advancementId));
        if (advancement == null) {
            return false;
        }
        var playerAdvancements = player.getAdvancements();
        AdvancementProgress progress = playerAdvancements.getOrStartProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) {
            playerAdvancements.award(advancement, criterion);
        }
        return true;
    }
}

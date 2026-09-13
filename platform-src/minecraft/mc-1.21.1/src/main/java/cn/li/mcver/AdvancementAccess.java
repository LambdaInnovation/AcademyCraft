package cn.li.mcver;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Cross-version advancement grant helpers.
 * 1.21.1: AdvancementHolder + get.
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
        AdvancementHolder advancement = server.getAdvancements().get(ResourceLocations.parse(advancementId));
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

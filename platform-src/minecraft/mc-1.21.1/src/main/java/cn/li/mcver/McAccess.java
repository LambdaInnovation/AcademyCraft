package cn.li.mcver;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Cross-version accessors for player/level/server APIs that drift by mapping.
 * Contract matches the 26.2 surface; older versions implement via classic getters.
 *
 * Server-safe only: methods whose signatures mention client-only types live in
 * {@link McClientAccess} so this class stays loadable on a dedicated server.
 */
public final class McAccess {
    private McAccess() {
    }

    public static ResourceLocation resourceKeyId(ResourceKey<?> key) {
        return key.location();
    }

    public static String resourceKeyString(ResourceKey<?> key) {
        return key.location().toString();
    }

    public static MinecraftServer serverOf(Player player) {
        return player == null ? null : player.getServer();
    }

    public static MinecraftServer serverOf(ServerPlayer player) {
        return serverOf((Player) player);
    }

    public static long dayTime(Level level) {
        return level.getDayTime();
    }

    /** In-game time ticks (pauses with the world). Prefer over wall-clock for logic. */
    public static long gameTime(Level level) {
        return level == null ? 0L : level.getGameTime();
    }

    public static String dimensionId(Level level) {
        return resourceKeyString(level.dimension());
    }

    public static int serverTickCount(MinecraftServer server) {
        return server == null ? 0 : server.getTickCount();
    }

    public static boolean isClientSide(Level level) {
        return level != null && level.isClientSide;
    }

    public static boolean hasCommandPermission(CommandSourceStack source, int level) {
        return source != null && source.hasPermission(level);
    }

}

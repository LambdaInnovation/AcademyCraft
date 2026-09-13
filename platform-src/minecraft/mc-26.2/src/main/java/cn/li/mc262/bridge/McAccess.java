package cn.li.mc262.bridge;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * @deprecated Use {@link cn.li.mcver.McAccess}. Thin alias for call-site migration.
 */
@Deprecated
public final class McAccess {
    private McAccess() {
    }

    public static Identifier resourceKeyId(ResourceKey<?> key) {
        return cn.li.mcver.McAccess.resourceKeyId(key);
    }

    public static String resourceKeyString(ResourceKey<?> key) {
        return cn.li.mcver.McAccess.resourceKeyString(key);
    }

    public static MinecraftServer serverOf(Player player) {
        return cn.li.mcver.McAccess.serverOf(player);
    }

    public static MinecraftServer serverOf(ServerPlayer player) {
        return cn.li.mcver.McAccess.serverOf(player);
    }

    public static long dayTime(Level level) {
        return cn.li.mcver.McAccess.dayTime(level);
    }

    public static long gameTime(Level level) {
        return cn.li.mcver.McAccess.gameTime(level);
    }

    public static String dimensionId(Level level) {
        return cn.li.mcver.McAccess.dimensionId(level);
    }

    public static int serverTickCount(MinecraftServer server) {
        return cn.li.mcver.McAccess.serverTickCount(server);
    }

    public static boolean hasCommandPermission(CommandSourceStack source, int level) {
        return cn.li.mcver.McAccess.hasCommandPermission(source, level);
    }
}

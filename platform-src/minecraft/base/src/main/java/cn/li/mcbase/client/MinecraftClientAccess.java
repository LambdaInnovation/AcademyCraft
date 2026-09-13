package cn.li.mcbase.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

/**
 * Shared client-side access helpers for loader-agnostic GUI/render code.
 *
 * <p>Callers must still ensure these methods are only reached from client-only
 * execution paths; this class deliberately avoids loader-specific annotations so
 * it can live in the shared mcbase module.</p>
 */
public final class MinecraftClientAccess {
    private MinecraftClientAccess() {
    }

    public static Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }

    public static Font getFont() {
        return Minecraft.getInstance().font;
    }
}
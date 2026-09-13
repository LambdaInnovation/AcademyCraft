package cn.li.mcver;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Client-only accessors for player/level/server APIs that drift by mapping.
 *
 * Deliberately separate from {@link McAccess}: the dedicated-server dist cannot
 * load client-typed classes ({@code @OnlyIn(CLIENT)}), and Clojure compiles
 * static-method calls by reflecting over every public method of the class
 * (Compiler$StaticMethodExpr -> Class.getMethods), which would pull Window /
 * Minecraft onto the server and fail dist checks. Any method whose signature
 * mentions a client-only type belongs here, never in McAccess.
 */
public final class McClientAccess {
    private McClientAccess() {
    }

    public static long windowHandle(Window window) {
        return window == null ? 0L : window.getWindow();
    }

    /** Client frame partial tick (Gui clock / render interpolation). */
    public static double clientPartialTick(Minecraft mc) {
        if (mc == null) {
            return 0.0d;
        }
        // 1.21.1: getTimer(); 26.2 renamed to getDeltaTracker().
        var tracker = mc.getTimer();
        return tracker == null ? 0.0d : tracker.getGameTimeDeltaPartialTick(false);
    }

    /**
     * Render-frame pose of the local player: the partial-tick interpolated
     * position/orientation the character model is drawn at. Body-anchored
     * level effects (storm-wing tornadoes etc.) must anchor to this pose, or
     * they run ahead of the rendered character by up to one tick of travel
     * while moving — the upstream StormWingEffect is a world entity whose
     * renderer interpolates between the same two player tick positions, so it
     * always tracks the model. Returns {x, y, z, eyeY, yawDeg, pitchDeg,
     * bodyYawDeg}.
     */
    public static double[] playerRenderPosition(LocalPlayer player, double partialTick) {
        double x = player.xo + (player.getX() - player.xo) * partialTick;
        double y = player.yo + (player.getY() - player.yo) * partialTick;
        double z = player.zo + (player.getZ() - player.zo) * partialTick;
        double eyeY = y + (player.getEyeY() - player.getY());
        double yaw = player.yRotO + (player.getYRot() - player.yRotO) * partialTick;
        double pitch = player.xRotO + (player.getXRot() - player.xRotO) * partialTick;
        double bodyYaw = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * partialTick;
        return new double[] {x, y, z, eyeY, yaw, pitch, bodyYaw};
    }

    /** Close the current screen if any (Minecraft.screen vs Minecraft.gui.screen). */
    public static void closeScreen(Minecraft mc) {
        if (mc != null && mc.screen != null) {
            mc.setScreen(null);
        }
    }

    /** Open or replace the current screen. */
    public static void setScreen(Minecraft mc, net.minecraft.client.gui.screens.Screen screen) {
        if (mc != null) {
            mc.setScreen(screen);
        }
    }

    /**
     * Client-side live snapshot of a loaded entity (position + bounding box),
     * for skill aim markers that must follow a target entity every frame
     * (upstream EntityMarker.target follow). Returns null when the entity is
     * not loaded on this client.
     */
    public static java.util.Map<String, Object> clientEntitySnapshot(java.util.UUID uuid) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        net.minecraft.client.multiplayer.ClientLevel level = mc.level;
        if (level == null || uuid == null) {
            return null;
        }
        // ClientLevel.getEntities() is protected, so the entity store is not
        // reachable from here — iterate the client's render list and match the
        // UUID, exactly as the 1.20.1 seam does.
        net.minecraft.world.entity.Entity entity = null;
        for (net.minecraft.world.entity.Entity candidate : level.entitiesForRendering()) {
            if (uuid.equals(candidate.getUUID())) {
                entity = candidate;
                break;
            }
        }
        if (entity == null) {
            return null;
        }
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("x", entity.getX());
        result.put("y", entity.getY());
        result.put("z", entity.getZ());
        result.put("width", entity.getBbWidth());
        result.put("height", entity.getBbHeight());
        return result;
    }

    /**
     * Client-side motion override for a loaded entity. VecReflection's
     * c_reflectEntity re-runs the reflection on the client so a bounced arrow
     * turns the instant the message lands, instead of holding its old course
     * until the next velocity sync. The server sends the velocity it already
     * computed, so the two sides cannot disagree about the new direction.
     *
     * No-op when the entity is not loaded here.
     */
    public static boolean setClientEntityMotion(java.util.UUID uuid, double vx, double vy, double vz) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        net.minecraft.client.multiplayer.ClientLevel level = mc.level;
        if (level == null || uuid == null) {
            return false;
        }
        for (net.minecraft.world.entity.Entity candidate : level.entitiesForRendering()) {
            if (uuid.equals(candidate.getUUID())) {
                candidate.setDeltaMovement(vx, vy, vz);
                candidate.hasImpulse = true;
                return true;
            }
        }
        return false;
    }
}

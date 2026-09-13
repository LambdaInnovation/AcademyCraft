package cn.li.mcver;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;

import java.util.EnumSet;
import java.util.Set;

/**
 * Cross-version entity teleport helpers.
 * 26.2: unified teleportTo(ServerLevel, x,y,z, Set&lt;Relative&gt;, yRot, xRot, boolean).
 */
public final class TeleportAccess {
    private static final Set<Relative> NO_RELATIVE = EnumSet.noneOf(Relative.class);

    private TeleportAccess() {
    }

    /**
     * Teleport {@code entity} onto {@code target} at absolute coords, preserving rotation.
     */
    public static boolean teleportPreservingRotation(Entity entity, ServerLevel target,
                                                     double x, double y, double z) {
        if (entity == null || target == null) {
            return false;
        }
        entity.teleportTo(target, x, y, z, NO_RELATIVE, entity.getYRot(), entity.getXRot(), false);
        return true;
    }
}

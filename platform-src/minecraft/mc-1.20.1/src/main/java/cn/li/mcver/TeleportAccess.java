package cn.li.mcver;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;

import java.util.EnumSet;
import java.util.Set;

/**
 * Cross-version entity teleport helpers.
 * 1.20.1 / 1.21.1: Entity.teleportTo(ServerLevel,...,Set&lt;RelativeMovement&gt;,...).
 * 26.2: Relative + trailing boolean.
 */
public final class TeleportAccess {
    private static final Set<RelativeMovement> NO_RELATIVE = EnumSet.noneOf(RelativeMovement.class);

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
        return entity.teleportTo(target, x, y, z, NO_RELATIVE, entity.getYRot(), entity.getXRot());
    }
}
